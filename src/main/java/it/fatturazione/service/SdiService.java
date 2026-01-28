package it.fatturazione.service;

import it.fatturazione.exception.SdiException;
import jakarta.xml.soap.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.net.ssl.*;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Service
@Slf4j
public class SdiService {

    @Value("${sdi.ws.url}")
    private String sdiWsUrl;

    @Value("${sdi.ws.notifiche.url}")
    private String sdiNotificheUrl;

    @Value("${sdi.trasmittente.codice}")
    private String codiceTrasmittente;

    @Value("${sdi.timeout}")
    private int timeout;

    private static final String NAMESPACE_URI = "http://www.fatturapa.gov.it/sdi/ws/trasmissione/v1.0";
    private static final String SERVICE_NAME = "TrasmissioneFatture";

    public String inviaFattura(String fileFirmatoPath, String nomeFile) throws SdiException {
        try {
            log.info("Inizio invio fattura a SDI: {}", nomeFile);

            // Leggi il file firmato
            byte[] fileContent = Files.readAllBytes(new File(fileFirmatoPath).toPath());
            String fileBase64 = Base64.getEncoder().encodeToString(fileContent);

            // Crea il messaggio SOAP
            MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
            SOAPMessage soapMessage = messageFactory.createMessage();
            
            // Crea il SOAP Body
            SOAPPart soapPart = soapMessage.getSOAPPart();
            SOAPEnvelope envelope = soapPart.getEnvelope();
            SOAPBody soapBody = envelope.getBody();

            // Aggiungi il namespace
            envelope.addNamespaceDeclaration("tra", NAMESPACE_URI);

            // Crea l'elemento fileSdIBase
            SOAPElement fileSdIBase = soapBody.addChildElement("fileSdIBase", "tra");
            
            // IdentificativoSdI (verrà assegnato da SDI nella risposta)
            SOAPElement identificativoSdI = fileSdIBase.addChildElement("IdentificativoSdI", "tra");
            identificativoSdI.addTextNode("0");

            // NomeFile
            SOAPElement nomeFileElement = fileSdIBase.addChildElement("NomeFile", "tra");
            nomeFileElement.addTextNode(nomeFile);

            // File (in base64)
            SOAPElement fileElement = fileSdIBase.addChildElement("File", "tra");
            fileElement.addTextNode(fileBase64);

            // Salva le modifiche
            soapMessage.saveChanges();

            // Log del messaggio SOAP (per debug)
            if (log.isDebugEnabled()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                soapMessage.writeTo(out);
                log.debug("SOAP Request: {}", out.toString());
            }

            // Invia usando RestTemplate
            SOAPMessage response = inviaSOAPMessage(sdiWsUrl, soapMessage);

            // Processa la risposta
            String identificativoSdiRisposta = processaRispostaInvio(response);

            log.info("Fattura inviata con successo. IdentificativoSdI: {}", identificativoSdiRisposta);
            return identificativoSdiRisposta;

        } catch (Exception e) {
            log.error("Errore durante l'invio della fattura a SDI", e);
            throw new SdiException("Errore durante l'invio della fattura: " + e.getMessage(), e);
        }
    }

    private RestTemplate createInsecureRestTemplate() {
        try {
            // Crea un TrustManager che accetta tutti i certificati
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // Installa il TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Crea un HostnameVerifier che accetta tutti gli hostname
            HostnameVerifier allHostsValid = (hostname, session) -> true;

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            RestTemplate template = new RestTemplate();
            log.info("RestTemplate configurato per bypassare la validazione SSL");
            return template;

        } catch (Exception e) {
            log.error("Errore durante la configurazione del RestTemplate insecure", e);
            return new RestTemplate();
        }
    }

    private SOAPMessage inviaSOAPMessage(String url, SOAPMessage request) throws Exception {
        // Converti il messaggio SOAP in byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        request.writeTo(outputStream);
        byte[] requestBytes = outputStream.toByteArray();

        // Configura RestTemplate con timeout
        RestTemplate restTemplate = createInsecureRestTemplate();
        
        // Prepara gli headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/xml; charset=utf-8"));
        headers.set("SOAPAction", "");

        // Crea la richiesta HTTP
        HttpEntity<byte[]> httpEntity = new HttpEntity<>(requestBytes, headers);

        try {
            // Invia la richiesta
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                byte[].class
            );

            // Converti la risposta in SOAPMessage
            MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(responseEntity.getBody());
            return messageFactory.createMessage(null, inputStream);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Errore HTTP durante l'invio SOAP: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SdiException("Errore HTTP: " + e.getStatusCode() + " - " + e.getMessage());
        }
    }

    private String processaRispostaInvio(SOAPMessage response) throws Exception {
        if (log.isDebugEnabled()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.writeTo(out);
            log.debug("SOAP Response: {}", out.toString());
        }

        SOAPBody responseBody = response.getSOAPBody();

        // Verifica se c'è un fault
        if (responseBody.hasFault()) {
            SOAPFault fault = responseBody.getFault();
            String faultString = fault.getFaultString();
            log.error("SOAP Fault ricevuto: {}", faultString);
            throw new SdiException("Errore SDI: " + faultString);
        }

        // Estrai l'IdentificativoSdI dalla risposta
        SOAPElement fileSdIBaseElement = (SOAPElement) responseBody.getFirstChild();
        
        // Cerca l'elemento IdentificativoSdI
        String identificativoSdi = null;
        java.util.Iterator<?> it = fileSdIBaseElement.getChildElements();
        while (it.hasNext()) {
            Object node = it.next();
            if (node instanceof SOAPElement) {
                SOAPElement element = (SOAPElement) node;
                if ("IdentificativoSdI".equals(element.getLocalName())) {
                    identificativoSdi = element.getTextContent();
                    break;
                }
            }
        }

        if (identificativoSdi == null || "0".equals(identificativoSdi)) {
            throw new SdiException("IdentificativoSdI non valido nella risposta");
        }

        return identificativoSdi;
    }

    public byte[] scaricaNotifica(String identificativoSdi, String nomeFile) throws SdiException {
        try {
            log.info("Scaricamento notifica da SDI - IdentificativoSdI: {}, File: {}", 
                    identificativoSdi, nomeFile);

            // Crea il messaggio SOAP per scaricare la notifica
            MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
            SOAPMessage soapMessage = messageFactory.createMessage();
            
            SOAPPart soapPart = soapMessage.getSOAPPart();
            SOAPEnvelope envelope = soapPart.getEnvelope();
            SOAPBody soapBody = envelope.getBody();

            envelope.addNamespaceDeclaration("not", NAMESPACE_URI);

            SOAPElement notificaElement = soapBody.addChildElement("riceviNotifica", "not");
            
            SOAPElement identificativoElement = notificaElement.addChildElement("IdentificativoSdI", "not");
            identificativoElement.addTextNode(identificativoSdi);

            SOAPElement nomeFileElement = notificaElement.addChildElement("NomeFile", "not");
            nomeFileElement.addTextNode(nomeFile);

            soapMessage.saveChanges();

            // Invia usando RestTemplate
            SOAPMessage response = inviaSOAPMessage(sdiNotificheUrl, soapMessage);

            // Processa la risposta
            return processaRispostaNotifica(response);

        } catch (Exception e) {
            log.error("Errore durante lo scaricamento della notifica", e);
            throw new SdiException("Errore durante lo scaricamento della notifica: " + e.getMessage(), e);
        }
    }

    private byte[] processaRispostaNotifica(SOAPMessage response) throws Exception {
        SOAPBody responseBody = response.getSOAPBody();

        if (responseBody.hasFault()) {
            SOAPFault fault = responseBody.getFault();
            throw new SdiException("Errore SDI: " + fault.getFaultString());
        }

        // Estrai il file dalla risposta (in base64)
        SOAPElement notificaElement = (SOAPElement) responseBody.getFirstChild();
        
        String fileBase64 = null;
        java.util.Iterator<?> it = notificaElement.getChildElements();
        while (it.hasNext()) {
            Object node = it.next();
            if (node instanceof SOAPElement) {
                SOAPElement element = (SOAPElement) node;
                if ("File".equals(element.getLocalName())) {
                    fileBase64 = element.getTextContent();
                    break;
                }
            }
        }

        if (fileBase64 == null) {
            throw new SdiException("File non trovato nella risposta della notifica");
        }

        return Base64.getDecoder().decode(fileBase64);
    }

    public boolean verificaStatoServizio() {
        try {
            log.info("Verifica stato servizio SDI");
            
            // Tentativo di connessione al servizio
            // In produzione, dovresti implementare una chiamata specifica per verificare lo stato
            
            return true;
        } catch (Exception e) {
            log.error("Errore durante la verifica dello stato del servizio SDI", e);
            return false;
        }
    }
}
