package dossierbox;
/**
 * DossierBrowserInfrastructure:
 * - Esta clase está completa y no debe ser modificada.
 * - Implementa el bucle de escucha de peticiones de clientes.
 * - Incluye mecanismos que esperan a que todas las transacciones en curso
 *   de clientes hayan terminado antes de proceder a detener el servicio.
 * 
 * @author MAZ
 * 
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;

final class DossierBoxInfrastructure implements Runnable {
  
  static private final String CLASS_NAME = DossierBoxInfrastructure.class.getName();
  static private final Logger LOGGER = Logger.getLogger(CLASS_NAME);
  
  // Sujeto autenticado (administrador)
  private final Subject subjectAdmin;  
  // Servicio ejecutor con hebras de ejecución para atender transacciones entrantes.
  final ExecutorService executorForInputTransactions;
  // Puerto de escucha de solicitudes de clientes
  final int port;
  
  protected DossierBoxInfrastructure (final Subject subjectAdmin, final int port, final int numThreads) {
    this.subjectAdmin = subjectAdmin;
    this.port = port;    
    this.executorForInputTransactions = Executors.newFixedThreadPool(numThreads);
  }
  
  void start () {
    // Ejecutor con la hebra para ejecutar el metodo run().
    final ExecutorService auxiliaryExecutor = Executors.newSingleThreadExecutor();
    // Se pone en ejecucion el metodo run().
    auxiliaryExecutor.submit(this);
  }

  @Override
  public void run () {

    // Necesario para poder ejecutar Subject.doAsPrivileged()
    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {

        return Subject.doAsPrivileged(subjectAdmin, (PrivilegedAction<Void>) () -> {
          
          try (final ServerSocket serverSocket = new ServerSocket(port)) {
            
            LOGGER.info("Componente DossierBrowserComponent en operación");

            // Servicio ejecutor para implementar timeout en transacciones entrantes.
            final ExecutorService auxiliarExecutor = Executors.newSingleThreadExecutor();

            do { // Bucle de escucha

              Socket socket = null;
              // Se reutiliza la tarea hasta que se complete
              final Future<Socket> future = auxiliarExecutor.submit(serverSocket::accept);
              do {

                try {
                  // Interrumpe la espera cada 10 miliseundos
                  // para saber si se ha detenido el servicio.
                  socket = future.get(10, MILLISECONDS);
                } catch (final InterruptedException | ExecutionException ex) {
                  // Aqui es obligado discriminar y tratar excepciones
                } catch (final TimeoutException ex) {}

              } while ((!future.isDone()) && (!executorForInputTransactions.isShutdown()));

              if (future.isDone()) {
                LOGGER.info("Transaccion entrante");
                final ServerTask task = new ServerTask(socket);
                executorForInputTransactions.submit(task);
              }

            } while (!executorForInputTransactions.isShutdown());

            // Bucle que espera a terminar todas las transacciones en curso.
            do {
              try {
                // Consulta cada 2 segundos si todavia quedan tareas de clientes activas.
                final boolean x = executorForInputTransactions.awaitTermination(2000, MILLISECONDS);
              } catch (final InterruptedException ex) {
                LOGGER.log(Level.SEVERE, "{0}", ex);
                return null;
              }
            } while (!executorForInputTransactions.isTerminated());

            LOGGER.info("Componente DossierBrowserComponent detenida");

          } catch (final IOException ex) {
            LOGGER.log(Level.SEVERE, "Server socket opening error {0}", ex);
          }
          
          return null;
          
        }, null);

      });

  }
  
  void stop () {
    
    // Necesario para poder ejecutar Subject.doAsPrivileged()
    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {

      return Subject.doAsPrivileged(subjectAdmin, (PrivilegedAction<Void>) () -> {

          executorForInputTransactions.shutdown();

          return null;

        }, null);
      
    });
   
  }

}