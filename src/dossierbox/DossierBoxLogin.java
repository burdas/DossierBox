package dossierbox;
/**
 * 
 * DossierBoxLogin. esta clase esta completamente definida
 *
 * @author MAZ
 */

import com.sun.security.auth.callback.TextCallbackHandler;
import java.util.Scanner;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

public final class DossierBoxLogin {

  static public void main (final String[] args) {
    
    if (args.length != 2) {
      System.out.println("Uso: dossierbox.DossierBoxLogin <PORT: int> <NUM_THREADS: int>");
      return;
    }
    
    final int servicePort = Integer.parseInt(args[0]);
    final int numThreads  = Integer.parseInt(args[1]);

    final LoginContext lc;
    try {
      lc = new LoginContext("DOSSIERBOX", new TextCallbackHandler());
    } catch (final LoginException ex) {
      System.err.println("No configuration entry to create specified LoginContext");
      return;
    } catch (final SecurityException ex) {
      System.err.println("No permission to create specified LoginContext");
      return;
    }

    try {

      // 1: intento de autenciacion.
      lc.login();

      // 2: se recupera el sujeto resultante de la autenticacion.
      final Subject subject = lc.getSubject();

      // 3: la infraestructura de servicio sólo se activa si se consigue una autenticacion positiva.
      // 3.a: DossierBoxInfrastructure es innaccesible desde fuera del paquete.
      final DossierBoxInfrastructure infrastructure =
              new DossierBoxInfrastructure(subject, servicePort, numThreads);

      // 3.b: la infraestructura se activa como usuario autenticado; se ejecuta
      //      mediante un servicio ejecutor sobre una hebra distinta a esta; desde
      //      esta hebra continua la ejecucion del menu.
      infrastructure.start();

      // 4: se presnta la interfaz de control de servicio
      show();

      // 5: parada de infraestructura del servicio; se completan tareas
      //    todavia en ejecucion, pero ya no se admiten más tareas.
      infrastructure.stop();

      // Se elimina sujeto autenticado y credenciales asociadas
      try {
        lc.logout();
      } catch (final LoginException ex) {
        System.err.println("Fallo al eliminar el contexto de login" + ex);
      }
      
      System.exit(0);      

    } catch (final LoginException ex) {
      System.err.println("Autenticación fallida en arranque de la aplicación");
    } catch (final SecurityException ex) {
      System.err.println("Sujeto no autorizado");
    }

  }
  
  static void show () {
    
    final Scanner scanner = new Scanner(System.in);
    int opcion; 
    do {
                   
      System.out.println("Opciones:");    
      System.out.println("  0 - Salir");
      System.out.print("Introduce opcion: ");
      opcion = scanner.nextInt();
      scanner.nextLine();
      
    } while (opcion != 0);
    
  }  

}