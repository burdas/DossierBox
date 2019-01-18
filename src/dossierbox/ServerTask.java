package dossierbox;

/**
 *
 * ServerTask: - Esta clase NO esta completamente definida. - Hay que
 * desarrollar un proceso que traslade las peticiones del cliente al navegador
 * que accede a los dossieres.
 *
 * @coauthor MAZ
 */
import dossierbox.browser.BrowserFactory;
import dossierbox.browser.DossierBrowser;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.rmi.AlreadyBoundException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import message.MessageProcessor;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.MessageProp;

final class ServerTask implements Runnable {

    static private final String CLASS_NAME = ServerTask.class.getName();
    static private final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    private final Socket socket;

    ServerTask(final Socket socket) {
        LOGGER.info("Creando tarea");
        this.socket = socket;
    }

    @Override
    public void run() {

        final MessageProcessor messageProcessor;
        try {
            messageProcessor = new MessageProcessor();
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return;
        }

        try (final DataInputStream is = new DataInputStream(socket.getInputStream());
                final DataOutputStream os = new DataOutputStream(socket.getOutputStream())) {

            // 1º: establecer contexto seguro JGSS con cliente
            GSSContext context = accept(is, os);
            if (context == null) {
                System.err.println("Problema en la creación de contexto");
                return;
            }
            // 2º: obtener browser según categoria de cliente autenticado
            DossierBrowser browser = BrowserFactory.getInstance(context);
            // 3º: sesión de interacción con cliente
            String opciones = is.readUTF();
            while (messageProcessor.isOPENMessage(opciones)) {
                final String dossierName = messageProcessor.parseOPENMessage(opciones);
                browser.open(dossierName);
                if (browser.isAvailable()) {
                    os.writeUTF(messageProcessor.getACKMessage());
                    os.flush();
                } else {
                    os.writeUTF(messageProcessor.getNACKMessage());
                    os.flush();
                    return;
                }
                final MessageProp prop = new MessageProp(0, true);
                String opciones2 = is.readUTF();
                do {
                    if (messageProcessor.isNEXTMessage(opciones2) || messageProcessor.isPREVIOUSMessage(opciones2)) {
                        os.writeUTF(messageProcessor.getACKMessage());
                        os.flush();
                        byte[] nodeData;
                        boolean next = browser.hasNext();
                        boolean previous = browser.hasPrevious();
                        if (messageProcessor.isNEXTMessage(opciones2)) {
                            nodeData = browser.next();
                        } else {
                            nodeData = browser.previous();
                        }
                        byte[] wrapNodeData = context.wrap(nodeData, 0, nodeData.length, prop);
                        os.writeUTF(messageProcessor.getNODEDATAMessage(wrapNodeData, next, previous));
                    } else {
                        os.writeUTF(messageProcessor.getNACKMessage());
                        os.flush();
                    }
                } while (messageProcessor.isCLOSEMessage(opciones2));
                browser.close();
                os.writeUTF(messageProcessor.getACKMessage());
                os.flush();
                opciones = is.readUTF();
            }
            if (messageProcessor.isBYEMessage(is.readUTF())) {
                System.out.println("Cerrando sesion");
            }

        } catch (final IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (GSSException ex) {
            LOGGER.log(Level.SEVERE, "Problema con el GSSContext", ex);
        } catch (GeneralSecurityException ex) {
            LOGGER.log(Level.SEVERE, "Problema de seguridad general", ex);
        } catch (AlreadyBoundException ex) {
            LOGGER.log(Level.SEVERE, "El fichero ya esta asignado a un browser", ex);
        }

    }

    private GSSContext accept(final DataInputStream is, final DataOutputStream os) throws GSSException {

        final GSSContext context;
        try {
            final GSSManager manager = GSSManager.getInstance();
            context = manager.createContext((GSSCredential) null);
        } catch (GSSException ex) {
            LOGGER.log(Level.SEVERE, "Problema al obtener el GSSContext", ex);
            return null;
        }
        try {
            while (!context.isEstablished()) {
                final byte[] inToken = new byte[is.readInt()];
                System.out.println("  * Will read input token of size "
                        + inToken.length
                        + " for processing by acceptSecContext");

                is.readFully(inToken);
                final byte[] outToken = context.acceptSecContext(inToken, 0, inToken.length);

                // Send a token to the peer if one was generated by acceptSecContext
                if (outToken != null) {

                    System.out.println("  * Will send token of size "
                            + outToken.length
                            + " from acceptSecContext.");
                    os.writeInt(outToken.length);
                    os.write(outToken);
                    os.flush();
                }
            }
            return context;
        } catch (GSSException ex) {
            LOGGER.log(Level.SEVERE, "Problem with GSSContext:", ex);
            return null;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Problem with socket:", ex);
            return null;
        }
    }

}
