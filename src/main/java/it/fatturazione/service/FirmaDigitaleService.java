package it.fatturazione.service;

import it.fatturazione.exception.FirmaDigitaleException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FirmaDigitaleService {

    @Value("${firma.keystore.path}")
    private String keystorePath;

    @Value("${firma.keystore.password}")
    private String keystorePassword;

    @Value("${firma.key.alias}")
    private String keyAlias;

    @Value("${firma.key.password}")
    private String keyPassword;

    private KeyStore keyStore;
    private PrivateKey privateKey;
    private X509Certificate certificate;

    @PostConstruct
    public void init() {
        Security.addProvider(new BouncyCastleProvider());
        try {
            caricaCertificato();
        } catch (Exception e) {
            log.warn("Impossibile caricare il certificato all'avvio: {}. Verrà caricato al primo utilizzo.", 
                    e.getMessage());
        }
    }

    private void caricaCertificato() throws Exception {
        log.info("Caricamento certificato da: {}", keystorePath);
        
        keyStore = KeyStore.getInstance("PKCS12");
        
        try (InputStream is = getKeystoreInputStream()) {
            keyStore.load(is, keystorePassword.toCharArray());
        }

        Key key = keyStore.getKey(keyAlias, keyPassword.toCharArray());
        if (!(key instanceof PrivateKey)) {
            throw new FirmaDigitaleException("La chiave non è una chiave privata");
        }
        privateKey = (PrivateKey) key;

        Certificate cert = keyStore.getCertificate(keyAlias);
        if (!(cert instanceof X509Certificate)) {
            throw new FirmaDigitaleException("Il certificato non è un X509Certificate");
        }
        certificate = (X509Certificate) cert;

        log.info("Certificato caricato con successo. Subject: {}", certificate.getSubjectX500Principal());
    }

    private InputStream getKeystoreInputStream() throws IOException {
        if (keystorePath.startsWith("classpath:")) {
            String path = keystorePath.substring("classpath:".length());
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            if (is == null) {
                throw new FileNotFoundException("Keystore non trovato nel classpath: " + path);
            }
            return is;
        } else {
            return new FileInputStream(keystorePath);
        }
    }

    public String firmaFile(String inputFilePath) throws FirmaDigitaleException {
        try {
            // Verifica che il certificato sia caricato
            if (privateKey == null || certificate == null) {
                caricaCertificato();
            }

            log.info("Inizio firma del file: {}", inputFilePath);

            // Leggi il contenuto del file
                byte[] content = Files.readAllBytes(new File(inputFilePath).toPath());

            // Crea il CMSSignedDataGenerator
            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();

            // Crea il ContentSigner
            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider("BC")
                    .build(privateKey);

            // Aggiungi il firmatario
            generator.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                            new JcaDigestCalculatorProviderBuilder()
                                    .setProvider("BC")
                                    .build())
                            .build(contentSigner, certificate));

            // Aggiungi i certificati
            List<X509Certificate> certList = new ArrayList<>();
            certList.add(certificate);
            Store certs = new JcaCertStore(certList);
            generator.addCertificates(certs);

            // Genera il CMS signed data
            CMSTypedData msg = new CMSProcessableByteArray(content);
            CMSSignedData signedData = generator.generate(msg, true);

            // Salva il file firmato
            String outputFilePath = inputFilePath.replace(".xml", ".xml.p7m");
            try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
                fos.write(signedData.getEncoded());
            }

            log.info("File firmato con successo: {}", outputFilePath);
            return outputFilePath;

        } catch (Exception e) {
            log.error("Errore durante la firma del file", e);
            throw new FirmaDigitaleException("Errore durante la firma del file: " + e.getMessage(), e);
        }
    }

    public boolean verificaFirma(String filePath) {
        try {
            byte[] signedData = Files.readAllBytes(new File(filePath).toPath());
            CMSSignedData cms = new CMSSignedData(signedData);
            
            Store certStore = cms.getCertificates();
            SignerInformationStore signers = cms.getSignerInfos();
            
            for (SignerInformation signer : signers.getSigners()) {
                @SuppressWarnings("unchecked")
                java.util.Collection certCollection = certStore.getMatches(signer.getSID());
                
                if (!certCollection.isEmpty()) {
                    X509Certificate cert = (X509Certificate) certCollection.iterator().next();
                    
                    if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder()
                            .setProvider("BC")
                            .build(cert))) {
                        log.info("Firma valida per il certificato: {}", cert.getSubjectX500Principal());
                        return true;
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            log.error("Errore durante la verifica della firma", e);
            return false;
        }
    }

    public byte[] estraiContenuto(String fileFirmato) throws FirmaDigitaleException {
        try {
            byte[] signedData = Files.readAllBytes(new File(fileFirmato).toPath());
            CMSSignedData cms = new CMSSignedData(signedData);
            
            CMSProcessable signedContent = cms.getSignedContent();
            if (signedContent != null) {
                return (byte[]) signedContent.getContent();
            }
            
            throw new FirmaDigitaleException("Impossibile estrarre il contenuto dal file firmato");
        } catch (Exception e) {
            log.error("Errore durante l'estrazione del contenuto", e);
            throw new FirmaDigitaleException("Errore durante l'estrazione del contenuto: " + e.getMessage(), e);
        }
    }
}
