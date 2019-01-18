package dossierbox.browser;

import com.sun.security.jgss.GSSUtil;
import dossierbox.cathegory.BrowserQuery;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.Subject;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;

/**
 *
 * @author
 */
public final class BrowserFactory {

    static private final String CLASS_NAME = BrowserFactory.class.getName();
    static private final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    static public DossierBrowser getInstance(final GSSContext gssContext) throws GeneralSecurityException,
            GSSException/*,
            SecurityException*/ {
        try {
            // Obtenemos el sujeto que representa al administrador
            final Subject administrador = GSSUtil.createSubject(gssContext.getTargName(), null);
            // Ejecutamos lo posterior con los permisos del administrado
            return Subject.doAsPrivileged(administrador, (PrivilegedExceptionAction<DossierBrowser>) new PrivilegedExceptionAction<DossierBrowser>() {
                @Override
                public DossierBrowser run() throws GeneralSecurityException, GSSException {
                    try {
                        // Obtenemos la key del fichero "dossierbox.key"
                        final byte[] bytes = getKey();
                        if (bytes == null) {
                            return null;
                        }
                        // preparamos la infraestructura para desencriptar la información;
                        final SecretKeySpec key = new SecretKeySpec(bytes, "AES128");
                        final Cipher decrypter = Cipher.getInstance("AES128/CFB/PKCS5Padding");
                        decrypter.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(new byte[16]));

                        //Obtenemos el nombre del cliente
                        final String client = gssContext.getTargName().toString();

                        // Obtenemos el alcance y el nivel
                        final BrowserQuery bq = new BrowserQuery(client);
                        final String resultado = Subject.doAsPrivileged(administrador, bq, null);

                        // Obtenemos el browser
                        DossierBrowser db;
                        switch (resultado) {
                            case "A":
                                db = new A_Browser(administrador, decrypter);
                                break;
                            case "B1":
                                db = new B1_Browser(administrador, decrypter);
                                break;
                            case "B2":
                                db = new B2_Browser(administrador, decrypter);
                                break;
                            case "C1":
                                db = new C1_Browser(administrador, decrypter);
                                break;
                            case "C2":
                                db = new C2_Browser(administrador, decrypter);
                                break;
                            default:
                                db = null;
                                break;
                        }

                        return db;
                    } catch (NoSuchAlgorithmException ex) {
                        LOGGER.log(Level.SEVERE, "No se encontro el algoritmo de cifrado", ex);
                        throw new GeneralSecurityException(ex);
                    } catch (NoSuchPaddingException ex) {
                        LOGGER.log(Level.SEVERE, "No se encontro el relleno", ex);
                        throw new GeneralSecurityException(ex);
                    } catch (InvalidKeyException ex) {
                        LOGGER.log(Level.SEVERE, "Clave invalida", ex);
                        throw new GeneralSecurityException(ex);
                    } catch (GSSException ex) {
                        LOGGER.log(Level.SEVERE, "No se ha encontrado el nombre del cliente", ex);
                        throw ex;
                    }
                }
            }, null);
        } catch (GSSException ex) {
            LOGGER.log(Level.SEVERE, "Problema con el contexto GSS", ex);
            throw ex;
        } catch (PrivilegedActionException ex) {
            // Como ya hemos hecho log anteriormente aqui solo las lanzaremos al exterior
            Exception e = ex.getException();
            if (e instanceof GSSException) {
                throw (GSSException) e;
            } else {
                throw (GeneralSecurityException) e;
            }
        }
    }

    static private byte[] getKey() {
        final String path = System.getProperty("user.dir") + File.separator + "etc" + File.separator + "dossierbox.key";
        try (InputStream in = new FileInputStream(new File(path));) {
            final byte[] bytes = new byte[in.available()];        // longitud de la clave 16 octetos
            in.read(bytes);
            return bytes;
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "No se encontro el fichero de la clave", ex);
            return null;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Problema al abrir el fichero", ex);
            return null;
        }
    }

//    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, AlreadyBoundException, FileNotFoundException {
//        final byte[] bytes = getKey();
//        final SecretKeySpec key = new SecretKeySpec(bytes, "AES");
//        final SecretKeySpec key2 = new SecretKeySpec(bytes, "AES");
//        final Cipher encripter = Cipher.getInstance("AES/CFB/PKCS5Padding");
//        final Cipher decrypter = Cipher.getInstance("AES/CFB/PKCS5Padding");
//        encripter.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[16]));
//        decrypter.init(Cipher.DECRYPT_MODE, key2, new IvParameterSpec(new byte[16]));
//        final byte[] entrada = "Este es el ultimo nodo de ambito B y nivel de confidencialidad 2".getBytes();
//        final byte[] bytes2 = encripter.doFinal(entrada);
//        final byte[] entrada64 = Base64.getEncoder().encode(bytes2);
//        final String salida = new String(entrada64);
//        System.out.println(salida);
//        final byte[] bytes4 = salida.getBytes();
//        final byte[] salida64 = Base64.getDecoder().decode(bytes4);
//        final byte[] bytes3 = decrypter.doFinal(salida64);
//        System.out.println(new String(bytes3));
//          final byte[] bytes = getKey();
//          final SecretKeySpec key = new SecretKeySpec(bytes, "AES");
//          final Cipher decrypter = Cipher.getInstance("AES/CFB/PKCS5Padding");
//          decrypter.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(new byte[16]));
//          B2_Browser browser = new B2_Browser(null, decrypter);
//          browser.open("dossier1.xml");
//          while (browser.hasNext()){
//              System.out.println(new String(browser.next()));
//              //System.out.println(new String(browser.previous()));
//          }
//          
//          
//        
//    }

}
