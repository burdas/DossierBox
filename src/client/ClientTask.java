package client;

/**
 *
 * ClientTask: - Esta clase NO esta completamente definida. - Hay que
 * desarrollar una interfaz que permita al usuario interactuar con la tarea de
 * servicio y recibir la información de los dossieres consultados.
 *
 * @coauthor MAZ
 */
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.security.PrivilegedAction;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import message.MessageProcessor;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.MessageProp;
import org.ietf.jgss.Oid;

final class ClientTask implements PrivilegedAction<Void> {

    static private final String CLASS_NAME = ClientTask.class.getName();
    static private final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    private final InetAddress ipAddress;
    private final int servicePort;

    ClientTask(final InetAddress ipAddress, final int servicePort) {
        this.ipAddress = ipAddress;
        this.servicePort = servicePort;
    }

    @Override
    public Void run() {

        final MessageProcessor messageProcessor;
        try {
            messageProcessor = new MessageProcessor();
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return null;
        }

        try (final Socket socket = new Socket(ipAddress, servicePort)) {

            try (final DataInputStream is = new DataInputStream(socket.getInputStream());
                    final DataOutputStream os = new DataOutputStream(socket.getOutputStream())) {

                // 1º: establecer contexto seguro JGSS con tarea servidora
                GSSContext context = init(is, os);
                // 2º: sesión de interacción con servicio
                Scanner sc = new Scanner(System.in);
                System.out.println("Seleccione una de las opciones:");
                System.out.println("\t1 - Abrir dossier");
                System.out.println("\t2 - Salir");
                int opcion = sc.nextInt();
                while (opcion == 1) {
                    System.out.print("Instroduzca el nombre del dossier: ");
                    String dossierName = sc.nextLine();
                    os.writeUTF(messageProcessor.getOPENMessage(dossierName));
                    os.flush();
                    String respuesta = is.readUTF();
                    if (messageProcessor.isACKMessage(respuesta)) {
                        System.out.println("Se ha abierto el dossier con éxito");
                    } else {
                        System.out.println("Ha ocurrido un error al abrir el dossier");
                        return null;
                    }
                    int opcion2;
                    final MessageProp prop = new MessageProp(0, false);
                    do {
                        System.out.println("Seleccione una de las opciones:");
                        System.out.println("\t1 - Extraer el nodo siguiente");
                        System.out.println("\t2 - Extraer el nodo anterior");
                        System.out.println("\t3 - Cerrar el dossier");
                        opcion2 = sc.nextInt();
                        if (opcion2 == 1 || opcion2 == 2) {
                            if (opcion2 == 1) {
                                os.writeUTF(messageProcessor.getNEXTMessage(true));
                                os.flush();
                            }
                            if (opcion2 == 2) {
                                os.writeUTF(messageProcessor.getPREVIOUSMessage(true));
                                os.flush();
                            }
                            if (messageProcessor.isNACKMessage(is.readUTF())) {
                                System.err.println("Problema en la transmision");
                                return null;
                            }
                            System.out.println("Mensaje recibido correctamente");
                            byte[] bytes;
                            String nodeMessage = is.readUTF();
                            bytes = messageProcessor.parseDataOfNODEDATAMessage(nodeMessage);
                            boolean next = messageProcessor.parseNextOfNODEDATAMessage(nodeMessage);
                            boolean previous = messageProcessor.parsePreviousOfNODEDATAMessage(nodeMessage);
                            byte[] salidaUnWrap = context.unwrap(bytes, 0, bytes.length, prop);
                            System.out.println("Mensaje con datos de nodo");
                            System.out.println("* Datos encriptados recibidos:       " + new BigInteger(+1, salidaUnWrap));
                            System.out.println("* Hay un nodo consultable posterior: " + next);
                            System.out.println("* Hay un nodo consultable previo:    " + previous);
                        }
                    } while (opcion2 < 1 && opcion2 > 2);
                    System.out.println("Cerrando el dossier");
                    os.writeUTF(messageProcessor.getCLOSEMessage());
                    os.flush();
                    if (!messageProcessor.isACKMessage(is.readUTF())) {
                        System.err.println("Problema al cerrar el dossier");
                        return null;
                    }
                    System.out.println("Sossier cerrado correctamente");
                    System.out.println("Seleccione una de las opciones:");
                    System.out.println("\t1 - Abrir dossier");
                    System.out.println("\t2 - Salir");
                    opcion = sc.nextInt();
                }
                os.writeUTF(messageProcessor.getBYEMessage());
                os.flush();

            } catch (final IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } catch (GSSException ex) {
                LOGGER.log(Level.SEVERE, "Problema con el GSSContext", ex);
            }

        } catch (final IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        return null;

    }

    private GSSContext init(final DataInputStream is, final DataOutputStream os) throws GSSException {
        final GSSContext context;
        try {
            final GSSManager manager = GSSManager.getInstance();
            final Oid krb5PrincipalNameOid = new Oid("1.2.840.113554.1.2.2.1");
            final GSSName serviceName = manager.createName("dossierbox", krb5PrincipalNameOid);
            final Oid krb5Oid = new Oid("1.2.840.113554.1.2.2");
            context = manager.createContext(serviceName,
                    krb5Oid,
                    null,
                    GSSContext.DEFAULT_LIFETIME);
        } catch (GSSException ex) {
            LOGGER.log(Level.SEVERE, "Problema al intentar obtener el contexto", ex);
            return null;
        }
        try {
            context.requestMutualAuth(true);
            context.requestConf(true);
            context.requestInteg(true);

            // Do the context eastablishment loop
            for (byte[] inToken = new byte[0]; !context.isEstablished();) {

                // inToken is ignored on the first call
                final byte[] outToken = context.initSecContext(inToken, 0, inToken.length);

                // Send a token to the server if one was generated by initSecContext
                if (outToken != null) {
                    System.out.println("  * Will send token of size "
                            + outToken.length
                            + " from initSecContext.");
                    os.writeInt(outToken.length);
                    os.write(outToken);
                    os.flush();
                }

                // If the client is done with context establishment
                // then there will be no more tokens to read in this loop
                if (!context.isEstablished()) {
                    inToken = new byte[is.readInt()];
                    System.out.println("  * Will read input token of size "
                            + inToken.length
                            + " for processing by initSecContext");
                    is.readFully(inToken);
                }
            }
            return context;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Problem with socket:", ex);
            return null;
        }

    }

}
