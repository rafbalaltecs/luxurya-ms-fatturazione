package it.fatturazione.service;

import it.fatturazione.dto.FatturaRequestDTO;
import it.fatturazione.entity.Fattura;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class XmlGeneratorService {

    @Value("${fattura.storage.path}")
    private String storagePath;

    @Value("${sdi.trasmittente.codice}")
    private String codiceTrasmittente;

    private static final String FATTURAPA_VERSION = "FPR12";
    private static final String FORMATO_TRASMISSIONE = "FPR12";

    public String generaXml(FatturaRequestDTO request, Fattura fattura) throws Exception {
        log.info("Inizio generazione XML per fattura: {}", request.getNumeroFattura());

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // Root element
        Element root = doc.createElement("p:FatturaElettronica");
        root.setAttribute("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
        root.setAttribute("xmlns:p", "http://ivaservizi.agenziaentrate.gov.it/docs/xsd/fatture/v1.2");
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.setAttribute("versione", FATTURAPA_VERSION);
        root.setAttribute("xsi:schemaLocation", 
            "http://ivaservizi.agenziaentrate.gov.it/docs/xsd/fatture/v1.2 http://www.fatturapa.gov.it/export/fatturazione/sdi/fatturapa/v1.2/Schema_del_file_xml_FatturaPA_versione_1.2.xsd");
        doc.appendChild(root);

        // FatturaElettronicaHeader
        Element header = doc.createElement("FatturaElettronicaHeader");
        root.appendChild(header);

        // DatiTrasmissione
        aggiungiDatiTrasmissione(doc, header, request);

        // CedentePrestatore
        aggiungiCedentePrestatore(doc, header, request.getCedente());

        // CessionarioCommittente
        aggiungiCessionarioCommittente(doc, header, request.getCessionario());

        // FatturaElettronicaBody
        Element body = doc.createElement("FatturaElettronicaBody");
        root.appendChild(body);

        // DatiGenerali
        aggiungiDatiGenerali(doc, body, request);

        // DatiBeniServizi
        aggiungiDatiBeniServizi(doc, body, request);

        // DatiPagamento (opzionale)
        if (request.getDatiPagamento() != null) {
            aggiungiDatiPagamento(doc, body, request.getDatiPagamento());
        }

        // Salva il file XML
        String fileName = generaNomeFile(request.getNumeroFattura());
        String filePath = storagePath + "/" + fileName;
        
        File directory = new File(storagePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(filePath));
        transformer.transform(source, result);

        log.info("XML generato con successo: {}", filePath);
        return filePath;
    }

    private void aggiungiDatiTrasmissione(Document doc, Element header, FatturaRequestDTO request) {
        Element datiTrasmissione = doc.createElement("DatiTrasmissione");
        header.appendChild(datiTrasmissione);

        // IdTrasmittente
        Element idTrasmittente = doc.createElement("IdTrasmittente");
        datiTrasmissione.appendChild(idTrasmittente);
        
        Element idPaese = doc.createElement("IdPaese");
        idPaese.setTextContent("IT");
        idTrasmittente.appendChild(idPaese);
        
        Element idCodice = doc.createElement("IdCodice");
        idCodice.setTextContent(codiceTrasmittente);
        idTrasmittente.appendChild(idCodice);

        // ProgressivoInvio
        Element progressivo = doc.createElement("ProgressivoInvio");
        progressivo.setTextContent(generaProgressivoInvio());
        datiTrasmissione.appendChild(progressivo);

        // FormatoTrasmissione
        Element formato = doc.createElement("FormatoTrasmissione");
        formato.setTextContent(FORMATO_TRASMISSIONE);
        datiTrasmissione.appendChild(formato);

        // CodiceDestinatario
        Element codiceDestinatario = doc.createElement("CodiceDestinatario");
        String codice = request.getCessionario().getCodiceDestinatario();
        codiceDestinatario.setTextContent(codice != null && !codice.isEmpty() ? codice : "0000000");
        datiTrasmissione.appendChild(codiceDestinatario);

        // PEC Destinatario (se presente)
        if (request.getCessionario().getPec() != null && !request.getCessionario().getPec().isEmpty()) {
            Element pec = doc.createElement("PECDestinatario");
            pec.setTextContent(request.getCessionario().getPec());
            datiTrasmissione.appendChild(pec);
        }
    }

    private void aggiungiCedentePrestatore(Document doc, Element header, FatturaRequestDTO.CedenteDTO cedente) {
        Element cedentePrestatore = doc.createElement("CedentePrestatore");
        header.appendChild(cedentePrestatore);

        // DatiAnagrafici
        Element datiAnagrafici = doc.createElement("DatiAnagrafici");
        cedentePrestatore.appendChild(datiAnagrafici);

        // IdFiscaleIVA
        Element idFiscaleIVA = doc.createElement("IdFiscaleIVA");
        datiAnagrafici.appendChild(idFiscaleIVA);
        
        Element idPaese = doc.createElement("IdPaese");
        idPaese.setTextContent("IT");
        idFiscaleIVA.appendChild(idPaese);
        
        Element idCodice = doc.createElement("IdCodice");
        idCodice.setTextContent(cedente.getPartitaIva());
        idFiscaleIVA.appendChild(idCodice);

        // CodiceFiscale (se diverso da P.IVA)
        if (cedente.getCodiceFiscale() != null && !cedente.getCodiceFiscale().equals(cedente.getPartitaIva())) {
            Element codiceFiscale = doc.createElement("CodiceFiscale");
            codiceFiscale.setTextContent(cedente.getCodiceFiscale());
            datiAnagrafici.appendChild(codiceFiscale);
        }

        // Anagrafica
        Element anagrafica = doc.createElement("Anagrafica");
        datiAnagrafici.appendChild(anagrafica);
        
        Element denominazione = doc.createElement("Denominazione");
        denominazione.setTextContent(cedente.getDenominazione());
        anagrafica.appendChild(denominazione);

        // RegimeFiscale
        Element regimeFiscale = doc.createElement("RegimeFiscale");
        regimeFiscale.setTextContent("RF01"); // Regime ordinario
        datiAnagrafici.appendChild(regimeFiscale);

        // Sede
        Element sede = doc.createElement("Sede");
        cedentePrestatore.appendChild(sede);
        
        aggiungiIndirizzo(doc, sede, cedente.getIndirizzo(), cedente.getCap(), 
                         cedente.getComune(), cedente.getProvincia(), cedente.getNazione());

        // Contatti (opzionale)
        if (cedente.getTelefono() != null || cedente.getEmail() != null) {
            Element contatti = doc.createElement("Contatti");
            cedentePrestatore.appendChild(contatti);
            
            if (cedente.getTelefono() != null) {
                Element telefono = doc.createElement("Telefono");
                telefono.setTextContent(cedente.getTelefono());
                contatti.appendChild(telefono);
            }
            
            if (cedente.getEmail() != null) {
                Element email = doc.createElement("Email");
                email.setTextContent(cedente.getEmail());
                contatti.appendChild(email);
            }
        }
    }

    private void aggiungiCessionarioCommittente(Document doc, Element header, FatturaRequestDTO.CessionarioDTO cessionario) {
        Element cessionarioCommittente = doc.createElement("CessionarioCommittente");
        header.appendChild(cessionarioCommittente);

        // DatiAnagrafici
        Element datiAnagrafici = doc.createElement("DatiAnagrafici");
        cessionarioCommittente.appendChild(datiAnagrafici);

        // IdFiscaleIVA (se presente)
        if (cessionario.getPartitaIva() != null && !cessionario.getPartitaIva().isEmpty()) {
            Element idFiscaleIVA = doc.createElement("IdFiscaleIVA");
            datiAnagrafici.appendChild(idFiscaleIVA);
            
            Element idPaese = doc.createElement("IdPaese");
            idPaese.setTextContent("IT");
            idFiscaleIVA.appendChild(idPaese);
            
            Element idCodice = doc.createElement("IdCodice");
            idCodice.setTextContent(cessionario.getPartitaIva());
            idFiscaleIVA.appendChild(idCodice);
        }

        // CodiceFiscale
        Element codiceFiscale = doc.createElement("CodiceFiscale");
        codiceFiscale.setTextContent(cessionario.getCodiceFiscale());
        datiAnagrafici.appendChild(codiceFiscale);

        // Anagrafica
        Element anagrafica = doc.createElement("Anagrafica");
        datiAnagrafici.appendChild(anagrafica);
        
        Element denominazione = doc.createElement("Denominazione");
        denominazione.setTextContent(cessionario.getDenominazione());
        anagrafica.appendChild(denominazione);

        // Sede
        Element sede = doc.createElement("Sede");
        cessionarioCommittente.appendChild(sede);
        
        aggiungiIndirizzo(doc, sede, cessionario.getIndirizzo(), cessionario.getCap(), 
                         cessionario.getComune(), cessionario.getProvincia(), cessionario.getNazione());
    }

    private void aggiungiDatiGenerali(Document doc, Element body, FatturaRequestDTO request) {
        Element datiGenerali = doc.createElement("DatiGenerali");
        body.appendChild(datiGenerali);

        Element datiGeneraliDocumento = doc.createElement("DatiGeneraliDocumento");
        datiGenerali.appendChild(datiGeneraliDocumento);

        // TipoDocumento
        Element tipoDocumento = doc.createElement("TipoDocumento");
        tipoDocumento.setTextContent("TD01"); // Fattura
        datiGeneraliDocumento.appendChild(tipoDocumento);

        // Divisa
        Element divisa = doc.createElement("Divisa");
        divisa.setTextContent("EUR");
        datiGeneraliDocumento.appendChild(divisa);

        // Data
        Element data = doc.createElement("Data");
        data.setTextContent(request.getDataFattura().format(DateTimeFormatter.ISO_DATE));
        datiGeneraliDocumento.appendChild(data);

        // Numero
        Element numero = doc.createElement("Numero");
        numero.setTextContent(request.getNumeroFattura());
        datiGeneraliDocumento.appendChild(numero);

        // Importo Totale Documento
        BigDecimal totale = request.getRiepilogoIva().getImponibile()
                .add(request.getRiepilogoIva().getImposta());
        Element importoTotale = doc.createElement("ImportoTotaleDocumento");
        importoTotale.setTextContent(totale.setScale(2).toString());
        datiGeneraliDocumento.appendChild(importoTotale);
    }

    private void aggiungiDatiBeniServizi(Document doc, Element body, FatturaRequestDTO request) {
        Element datiBeniServizi = doc.createElement("DatiBeniServizi");
        body.appendChild(datiBeniServizi);

        // DettaglioLinee
        for (FatturaRequestDTO.DettaglioRigaDTO riga : request.getDettaglioRighe()) {
            Element dettaglioLinee = doc.createElement("DettaglioLinee");
            datiBeniServizi.appendChild(dettaglioLinee);

            addElement(doc, dettaglioLinee, "NumeroLinea", riga.getNumeroLinea().toString());
            addElement(doc, dettaglioLinee, "Descrizione", riga.getDescrizione());
            addElement(doc, dettaglioLinee, "Quantita", riga.getQuantita().setScale(2).toString());
            addElement(doc, dettaglioLinee, "UnitaMisura", riga.getUnitaMisura());
            addElement(doc, dettaglioLinee, "PrezzoUnitario", riga.getPrezzoUnitario().setScale(2).toString());
            
            BigDecimal prezzoTotale = riga.getQuantita().multiply(riga.getPrezzoUnitario());
            addElement(doc, dettaglioLinee, "PrezzoTotale", prezzoTotale.setScale(2).toString());
            addElement(doc, dettaglioLinee, "AliquotaIVA", riga.getAliquotaIva().setScale(2).toString());
        }

        // DatiRiepilogo
        Element datiRiepilogo = doc.createElement("DatiRiepilogo");
        datiBeniServizi.appendChild(datiRiepilogo);

        FatturaRequestDTO.RiepilogoIvaDTO riepilogo = request.getRiepilogoIva();
        addElement(doc, datiRiepilogo, "AliquotaIVA", riepilogo.getAliquotaIva().setScale(2).toString());
        
        if (riepilogo.getNatura() != null) {
            addElement(doc, datiRiepilogo, "Natura", riepilogo.getNatura());
        }
        
        addElement(doc, datiRiepilogo, "ImponibileImporto", riepilogo.getImponibile().setScale(2).toString());
        addElement(doc, datiRiepilogo, "Imposta", riepilogo.getImposta().setScale(2).toString());
        addElement(doc, datiRiepilogo, "EsigibilitaIVA", "I"); // Immediata
    }

    private void aggiungiDatiPagamento(Document doc, Element body, FatturaRequestDTO.DatiPagamentoDTO datiPagamento) {
        Element datiPagamentoElement = doc.createElement("DatiPagamento");
        body.appendChild(datiPagamentoElement);

        addElement(doc, datiPagamentoElement, "CondizioniPagamento", datiPagamento.getCondizioniPagamento());

        Element dettaglioPagamento = doc.createElement("DettaglioPagamento");
        datiPagamentoElement.appendChild(dettaglioPagamento);

        FatturaRequestDTO.DettaglioPagamentoDTO dettaglio = datiPagamento.getDettaglioPagamento();
        addElement(doc, dettaglioPagamento, "ModalitaPagamento", dettaglio.getModalitaPagamento());
        addElement(doc, dettaglioPagamento, "DataScadenzaPagamento", 
                  dettaglio.getDataScadenza().format(DateTimeFormatter.ISO_DATE));
        addElement(doc, dettaglioPagamento, "ImportoPagamento", 
                  dettaglio.getImportoPagamento().setScale(2).toString());

        if (dettaglio.getIban() != null) {
            addElement(doc, dettaglioPagamento, "IBAN", dettaglio.getIban());
        }
        
        if (dettaglio.getIstitutoFinanziario() != null) {
            addElement(doc, dettaglioPagamento, "IstitutoFinanziario", dettaglio.getIstitutoFinanziario());
        }
    }

    private void aggiungiIndirizzo(Document doc, Element parent, String indirizzo, String cap, 
                                   String comune, String provincia, String nazione) {
        addElement(doc, parent, "Indirizzo", indirizzo);
        addElement(doc, parent, "CAP", cap);
        addElement(doc, parent, "Comune", comune);
        addElement(doc, parent, "Provincia", provincia);
        addElement(doc, parent, "Nazione", nazione);
    }

    private void addElement(Document doc, Element parent, String name, String value) {
        Element element = doc.createElement(name);
        element.setTextContent(value);
        parent.appendChild(element);
    }

    private String generaNomeFile(String numeroFattura) {
        String numeroPulito = numeroFattura.replaceAll("[^a-zA-Z0-9]", "_");
        return "IT" + codiceTrasmittente + "_" + numeroPulito + ".xml";
    }

    private String generaProgressivoInvio() {
        return String.format("%05d", System.currentTimeMillis() % 100000);
    }
}
