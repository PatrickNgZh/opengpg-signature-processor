import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.Security;

/**
 * @author patrickng
 * @date 5/6/2021 1:18 AM
 */
public class SignatureProcessor {
    /**
     * verify detached armored signature file
     *
     * @param fileName original file
     * @param inputFileName signature file
     * @param keyFileName public key
     * @throws IOException IOException
     * @throws PGPException PGPException
     */
    public static void verifySignature(String fileName, String inputFileName, String keyFileName)
            throws IOException, PGPException {

        InputStream in = new BufferedInputStream(new FileInputStream(inputFileName));
        InputStream keyIn = new BufferedInputStream(new FileInputStream(keyFileName));

        verifySignature(fileName, in, keyIn);

        keyIn.close();
        in.close();
    }

    private static void verifySignature(String fileName, InputStream in, InputStream keyIn)
            throws IOException, PGPException {

        in = PGPUtil.getDecoderStream(in);

        JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(in);
        PGPSignatureList p3;

        Object o = pgpFact.nextObject();
        if (o instanceof PGPCompressedData) {
            PGPCompressedData c1 = (PGPCompressedData) o;

            pgpFact = new JcaPGPObjectFactory(c1.getDataStream());

            p3 = (PGPSignatureList) pgpFact.nextObject();
        } else {
            p3 = (PGPSignatureList) o;
        }

        PGPPublicKeyRingCollection pgpPubRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(keyIn), new JcaKeyFingerprintCalculator());


        InputStream dIn = new BufferedInputStream(new FileInputStream(fileName));

        PGPSignature sig = p3.get(0);
        PGPPublicKey key = pgpPubRingCollection.getPublicKey(sig.getKeyID());

        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), key);

        int ch;
        while ((ch = dIn.read()) >= 0) {
            sig.update((byte) ch);
        }

        dIn.close();

        if (sig.verify()) {
            System.out.println("signature verified.");
        } else {
            System.out.println("signature verification failed.");
        }
    }

    /**
     * create detached armored signature file
     *
     * @param inputFileName original file
     * @param keyFileName private key
     * @param outputFileName signature file
     * @param pass password
     * @param armor armored
     * @throws IOException IOException
     * @throws PGPException PGPException
     */
    public static void createSignature(String inputFileName, String keyFileName,
                                       String outputFileName, char[] pass, boolean armor)
            throws IOException, PGPException {

        InputStream keyIn = new BufferedInputStream(new FileInputStream(keyFileName));
        OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFileName));

        createSignature(inputFileName, keyIn, out, pass, armor);

        out.close();
        keyIn.close();
    }

    private static void createSignature(String fileName, InputStream keyIn, OutputStream out,
                                        char[] pass, boolean armor)
            throws IOException, PGPException {
        if (armor) {
            out = new ArmoredOutputStream(out);
        }

        PGPSecretKey pgpSec = PGPExampleUtil.readSecretKey(keyIn);
        PGPPrivateKey pgpPrivKey = pgpSec.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(pass));
        PGPSignatureGenerator sGen = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(pgpSec.getPublicKey().getAlgorithm(), PGPUtil.SHA256).setProvider("BC"));

        sGen.init(PGPSignature.BINARY_DOCUMENT, pgpPrivKey);

        BCPGOutputStream bOut = new BCPGOutputStream(out);

        InputStream fIn = new BufferedInputStream(new FileInputStream(fileName));

        int ch;
        while ((ch = fIn.read()) >= 0) {
            sGen.update((byte) ch);
        }

        fIn.close();

        sGen.generate().encode(bOut);

        if (armor) {
            out.close();
        }
    }

    public static void main(String[] args) throws PGPException, GeneralSecurityException, IOException {
        Security.addProvider(new BouncyCastleProvider());
//        SignatureProcessor.createSignature(fileName, privateKey, outputName, pass, true);
//        SignatureProcessor.verifySignature(fileName,outputName,publicKey);
    }
}
