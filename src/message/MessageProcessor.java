package message;
/**
 *
 * MessageProcessor: esta clase esta completamente definida.
 * 
 * @author MAZ
 */

import java.io.IOException;
import java.io.StringReader;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class MessageProcessor {
  
  static private final String CLASS_NAME = MessageProcessor.class.getName();
  static private final Logger LOGGER = Logger.getLogger(CLASS_NAME);  
    
  static public final String ACK_MESSAGE   = "<message type=\"ACK\"/>";
  static public final String NACK_MESSAGE  = "<message type=\"NACK\"/>";
  static public final String BYE_MESSAGE   = "<message type=\"BYE\"/>";
  static public final String CLOSE_MESSAGE = "<message type=\"CLOSE\"/>";
  static public final String NEXT_MESSAGE  = "<message type=\"NEXT\"/>";
  static public final String PREVIOUS_MESSAGE = "<message type=\"PREVIOUS\"/>";  
  
  private final DocumentBuilder docBuilder;
  private final Base64.Encoder encoder;
  private final Base64.Decoder decoder;
  
  public MessageProcessor () throws ParserConfigurationException {
    final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    this.docBuilder = dbFactory.newDocumentBuilder();
    this.encoder = Base64.getEncoder();
    this.decoder = Base64.getDecoder();
  }
 
  public String getOPENMessage (final String dossierName) {
    
    final String initTag = "<message type=\"OPEN\">\n";        
    final String valueElement = "<dossierName>" + dossierName + "</dossierName>\n";
    final String finishTag = "</message>";
    final String message = initTag + valueElement + finishTag;
    
    return message;    
  }
  
  public boolean isOPENMessage (final String message) {  
    final String messageType = parseTypeValue(message);
    return (messageType.compareTo("OPEN") == 0);
  }    
  
  public String parseOPENMessage (final String message) {

    if (!isOPENMessage(message)) {
      LOGGER.log(Level.SEVERE, "unexpected message type\n {0}", message);
      return "";
    }
          
    try {
      
      final StringReader is = new StringReader(message);
      final Document doc = docBuilder.parse(new InputSource(is));

      final NodeList items = doc.getElementsByTagName("message");
      final Node node = items.item(0);
      final Element element = (Element) node;
      
      final String dossierName =
              element.getElementsByTagName("dossierName").item(0).getTextContent();

      return dossierName;

    } catch (final SAXException ex) {
      LOGGER.log(Level.SEVERE, "unexpected message format\n {0}", message);
      return "";
    } catch (final IOException ex) {
      LOGGER.log(Level.SEVERE, "message error\n {0}", message);
      return "";
    }

  }
  
  public String getCLOSEMessage () { 
    return CLOSE_MESSAGE;    
  }
  
  public boolean isCLOSEMessage (final String message) {  
    final String messageType = parseTypeValue(message);
    return (messageType.compareTo("CLOSE") == 0);
  }  
  
  public String getACKMessage () { 
    return ACK_MESSAGE;    
  }
  
  public boolean isACKMessage (final String message) {
    final String messageType = parseTypeValue(message);
    return (messageType.compareTo("ACK") == 0);
  }
  
  public String getNACKMessage () { 
    return NACK_MESSAGE;    
  }
  
  public boolean isNACKMessage (final String message) {
    final String messageType = parseTypeValue(message);
    return (messageType.compareTo("NACK") == 0);
  } 
  
  public String getBYEMessage () { 
    return BYE_MESSAGE;    
  }
  
  public boolean isBYEMessage (final String message) {
    final String messageType = parseTypeValue(message);
    return (messageType.compareTo("BYE") == 0);
  }
  
  public String getNEXTMessage (final boolean resultStatus) {
    return NEXT_MESSAGE;    
  }
  
  public boolean isNEXTMessage (final String message) {
    final String messageType = parseTypeValue(message);
    return (messageType.compareTo("NEXT") == 0);
  }
  
  public String getPREVIOUSMessage (final boolean resultStatus) {
    return PREVIOUS_MESSAGE;    
  }
  
  public boolean isPREVIOUSMessage (final String message) {
    final String messageType = parseTypeValue(message);
    return (messageType.compareTo("PREVIOUS") == 0);
  }  
    
  public String getNODEDATAMessage (final byte[] bytes, final boolean next, final boolean previous) {
    
    final String initTag = "<message type=\"NODEDATA\">\n";
    final String data = encoder.encodeToString(bytes);
    final String dataElement = "<data>" + data + "</data>\n";
    final String nextElement = "<next>" + (next ? "true" : "false") + "</next>\n"; 
    final String previousElement = "<previous>" + (previous ? "true" : "false") + "</previous>\n";     
    final String finishTag = "</message>";
    final String message = initTag +
                             dataElement +
                             nextElement +
                             previousElement +
                           finishTag;
    
    return message;    
  }
  
  public boolean isNODEDATAMessage (final String message) {
    final String messageType = parseTypeValue(message);
    return (messageType.compareTo("NODEDATA") == 0);
  }  
  
  public byte[] parseDataOfNODEDATAMessage (final String message) {
   
    if (!isNODEDATAMessage(message)) {
      LOGGER.log(Level.SEVERE, "unexpected message type\n {0}", message);
      return new byte[0];
    }
    
    try {
      
      final StringReader is = new StringReader(message);
      final Document doc = docBuilder.parse(new InputSource(is));

      final NodeList items = doc.getElementsByTagName("message");
      final Node node = items.item(0);
      final Element element = (Element) node;

      final String data =
              element.getElementsByTagName("data").item(0).getTextContent().toLowerCase();

      return (decoder.decode(data));

    } catch (final SAXException ex) {
      LOGGER.log(Level.SEVERE, "unexpected message format\n {0}", message);
      return null;
    } catch (final IOException ex) {
      LOGGER.log(Level.SEVERE, "message error\n {0}", message);
      return null;
    }
    
  }
  
  public boolean parseNextOfNODEDATAMessage (final String message) {
   
    if (!isNODEDATAMessage(message)) {
      LOGGER.log(Level.SEVERE, "unexpected message type\n {0}", message);
      return false;
    }
    
    try {
      
      final StringReader is = new StringReader(message);
      final Document doc = docBuilder.parse(new InputSource(is));

      final NodeList items = doc.getElementsByTagName("message");
      final Node node = items.item(0);
      final Element element = (Element) node;

      final String next =
              element.getElementsByTagName("next").item(0).getTextContent().toLowerCase();

      return (next.compareTo("true") == 0);

    } catch (final SAXException ex) {
      LOGGER.log(Level.SEVERE, "unexpected message format\n {0}", message);
      return false;
    } catch (final IOException ex) {
      LOGGER.log(Level.SEVERE, "message error\n {0}", message);
      return false;
    }
    
  }

  public boolean parsePreviousOfNODEDATAMessage (final String message) {
   
    if (!isNODEDATAMessage(message)) {
      LOGGER.log(Level.SEVERE, "unexpected message type\n {0}", message);
      return false;
    }
    
    try {
      
      final StringReader is = new StringReader(message);
      final Document doc = docBuilder.parse(new InputSource(is));

      final NodeList items = doc.getElementsByTagName("message");
      final Node node = items.item(0);
      final Element element = (Element) node;

      final String previous =
              element.getElementsByTagName("previous").item(0).getTextContent().toLowerCase();

      return (previous.compareTo("true") == 0);

    } catch (final SAXException ex) {
      LOGGER.log(Level.SEVERE, "unexpected message format\n {0}", message);
      return false;
    } catch (final IOException ex) {
      LOGGER.log(Level.SEVERE, "message error\n {0}", message);
      return false;
    }
    
  }  
  
  private String parseTypeValue (final String message) {
   
    try {
      
      final StringReader is = new StringReader(message);
      final Document doc = docBuilder.parse(new InputSource(is));

      final NodeList items = doc.getElementsByTagName("message");
      final Node node = items.item(0);
      final Element element = (Element) node;

      return element.getAttribute("type");

    } catch (final SAXException ex) {
      LOGGER.log(Level.SEVERE, "unexpected message format\n {0}", message);
      return "";
    } catch (final IOException ex) {
      LOGGER.log(Level.SEVERE, "message error\n {0}", message);
      return "";
    }
    
  }  
  
}