package dossierbox.browser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author
 */
public abstract class DossierBrowser {

    private static ArrayList<String> listaDossiers = new ArrayList<>();
    private String currentDossier;
    protected Document document;

    public void open(final String dossier) throws
            AlreadyBoundException,
            FileNotFoundException/*,
            SecurityException */{
        try {
            if (listaDossiers.contains(dossier)) {
                throw new AlreadyBoundException();
            } else {
                listaDossiers.add(dossier);
                currentDossier = dossier;
            }
            final String path
                    = System.getProperty("user.dir") + File.separator + "data" + File.separator + "repository" + File.separator;
            final File file = new File(path + dossier);
            if (!file.exists()) throw new FileNotFoundException("No se ha encontrado el fichero en <" + path + dossier + ">");
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            document = dBuilder.parse(file);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(DossierBrowser.class.getName()).log(Level.SEVERE, "Problema en el parser", ex);
            document = null;
        } catch (SAXException ex) {
            Logger.getLogger(DossierBrowser.class.getName()).log(Level.SEVERE, "Problema con el XML", ex);
            document = null;
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (IOException ex) {
            Logger.getLogger(DossierBrowser.class.getName()).log(Level.SEVERE, "Problema de entrada salida", ex);
            document = null;
        }
    }
    
    public abstract boolean hasNext ();
    public abstract byte[] next ();
    public abstract boolean hasPrevious ();
    public abstract byte[] previous ();
    
    public void close (){
        document = null;
        listaDossiers.remove(currentDossier);
    }
    
    public boolean isAvailable(){
        return document != null && document.getElementsByTagName("dossierBoxDocument").getLength() > 0;
    }
}
