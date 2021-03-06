package dossierbox.browser;

import com.sun.org.apache.xml.internal.serializer.ElemDesc;
import java.util.ArrayList;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.security.auth.Subject;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author 
 */

final class B2_Browser extends DossierBrowser {
    private final Subject Administrador;
    private final Cipher decrypter;
    
    private NodeList scopeNodeList;
    private ArrayList<Element> scopeWithLevel;
    
    private int currentIndex;
    private int previousIndex;

    public B2_Browser(Subject Administrador, Cipher decrypter) {
        this.Administrador = Administrador;
        this.decrypter = decrypter;
        scopeNodeList = null;
        scopeWithLevel = new ArrayList<>();
        currentIndex = -1;
        previousIndex = -1;
    }   

    @Override
    public boolean hasNext() {
        if (!isAvailable()){
            System.err.println("No se ha obtenido correctamente el contenido del fichero");
            return false;
        }
        if (scopeNodeList == null) {
            scopeNodeList = document.getElementsByTagName("scopeB");
            for (int i = 0; i < scopeNodeList.getLength(); i++) {
                Element elemento = (Element) scopeNodeList.item(i);
                if (elemento.getAttribute("confidencialityLevel").compareTo("2") == 0) {
                    scopeWithLevel.add(elemento);
                }
                
            }
        }
        return currentIndex < scopeWithLevel.size() - 1;
    }

    @Override
    public byte[] next() {
        try {
            if (!isAvailable()){
                System.err.println("No se ha obtenido correctamente el contenido del fichero");
                return new byte[0];
            }
            if (!hasNext()) {
                return new byte[0];
            }
            
            previousIndex = currentIndex;
            currentIndex += 1;
            
            Element element = scopeWithLevel.get(currentIndex);
            final byte[] text64 = Base64.getDecoder().decode(element.getTextContent());
            return decrypter.doFinal(text64);
            
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(A_Browser.class.getName()).log(Level.SEVERE, "Problema con el tamaño del bloque", ex);
            return new byte[0];
        } catch (BadPaddingException ex) {
            Logger.getLogger(A_Browser.class.getName()).log(Level.SEVERE, "Problema con el relleno", ex);
            return new byte[0];
        }
    }

    @Override
    public boolean hasPrevious() {
        if (!isAvailable()){
            System.err.println("No se ha obtenido correctamente el contenido del fichero");
            return false;
        }
        return previousIndex > -1;
    }

    @Override
    public byte[] previous() {
        try {
            if (!isAvailable()){
                System.err.println("No se ha obtenido correctamente el contenido del fichero");
                return null;
            }
            if (!hasPrevious()) {
                return new byte[0];
            }
            Element element = scopeWithLevel.get(previousIndex);
            final byte[] text64 = Base64.getDecoder().decode(element.getTextContent());
            return decrypter.doFinal(text64);
            
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(A_Browser.class.getName()).log(Level.SEVERE, "Problema con el tamaño del bloque", ex);
            return new byte[0];
        } catch (BadPaddingException ex) {
            Logger.getLogger(A_Browser.class.getName()).log(Level.SEVERE, "Problema con el relleno", ex);
            return new byte[0];
        }
    }
}