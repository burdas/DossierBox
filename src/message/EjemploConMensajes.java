package message;
/**
 *
 * Este ejemplo muestra como usar los metodos del procesador de mensajes.
 * Debes eliminar el ejemplo antes de entregar el trabajo.
 * 
 * @author MAZ
 */

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;
import javax.xml.parsers.ParserConfigurationException;

public final class EjemploConMensajes {
  
  static private final Random SRG = new SecureRandom();
  
  static public void main (final String[] args) throws ParserConfigurationException {
    
    // Objeto preparador de mensajes estandar a intercambiar
    // una vez establecida la asocicion segura entre cliente
    // y tarea servidora.
    final MessageProcessor messagesProcessor = new MessageProcessor();
        
    { // Ejemplo de preparacion y parseado de un mensaje OPEN
      
      // Mensaje preparado por cliente
      final String message = messagesProcessor.getOPENMessage("dossierSecretodePepa.xml");
      
      //
      // Transmisión del mensaje
      //
      
      // Proceso del mensaje recibido por la tarea servidora
      if (messagesProcessor.isOPENMessage(message)) {
        final String dossierName = messagesProcessor.parseOPENMessage(message);
        System.out.println("El nombre del dossier a procesar es " + dossierName);
      } else {
        System.out.println("El mensaje recibido no es de tipo OPEN");
      }
      
    }
    
    { // Ejemplo de preparación y parseado de un mensaje ACK
      
      // Mensaje preparado por cliente
      final String message = messagesProcessor.getACKMessage();
      
      //
      // Transmision del mensaje
      //
      
      // Proceso del mensaje recibido por la tarea servidora
      if (messagesProcessor.isACKMessage(message)) {
        System.out.println("El mensaje recibido es de confirmación positiva");
      } else {
        System.out.println("El mensaje recibido no es de confirmación positiva");
      }
      
    }
    
    { // Ejemplo de preparación y parseado de un mensaje ACK
      
      // Mensaje preparado por cliente
      final String message = messagesProcessor.getNACKMessage();
      
      //
      // Transmision del mensaje
      //
      
      // Proceso del mensaje recibido por la tarea servidora
      if (messagesProcessor.isNACKMessage(message)) {
        System.out.println("Pero este otro mensaje recibido es de confirmación negativa");
      } else {
        System.out.println("El mensaje recibido no es de confirmación negativa");
      }
      
    }
    
    { // Ejemplo de preparación y parseado de un mensaje NODEDATA
      
      // Mensaje preparado por cliente
      // Contenido encriptado del nodo
      final byte[] encryptedBytes = new byte[40];
      SRG.nextBytes(encryptedBytes);
      // No hay mas nodos consultables en el dossier
      final boolean next = SRG.nextBoolean();
      // Hay nodos previos consultables en el dossier
      final boolean previous = SRG.nextBoolean();
      final String message = messagesProcessor.getNODEDATAMessage(encryptedBytes, next, previous);
      
      //
      // Transmision del mensaje
      //
      
      // Proceso del mensaje recibido por la tarea servidora
      if (messagesProcessor.isNODEDATAMessage(message)) {
        final byte[] bytes = messagesProcessor.parseDataOfNODEDATAMessage(message);
        final boolean _next = messagesProcessor.parseNextOfNODEDATAMessage(message);
        final boolean _previous = messagesProcessor.parsePreviousOfNODEDATAMessage(message);
        System.out.println("Mensaje con datos de nodo");
        System.out.println("* Datos encriptados recibidos:       " + new BigInteger(+1, bytes));
        System.out.println("* Hay un nodo consultable posterior: " + _next);
        System.out.println("* Hay un nodo consultable previo:    " + _previous);
      } else {
        System.out.println("No es un mensaje de tipo NODEDATA");
      }
    }
    
    { // Ejemplo de preparación y parseado de un mensaje BYE
      
      // Mensaje preparado por cliente
      final String message = messagesProcessor.getBYEMessage();
      
      //
      // Transmision del mensaje
      //
      
      // Proceso del mensaje recibido por la tarea servidora
      if (messagesProcessor.isBYEMessage(message)) {
        System.out.println("Mensaje con el que una parte informa a la otra que va a cortar la sesión");
      } else {
        System.out.println("El mensaje recibido no es de final de sesión");
      }
      
    }    
    
  }

}