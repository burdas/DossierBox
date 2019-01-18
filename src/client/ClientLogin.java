package client;
/**
 *
 * ClientLogin: esta clase esta completamente definida.
 * 
 * @author MAZ
 */

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

public final class ClientLogin {

  static public void main (final String[] args) {
    
    if (args.length != 2) {
      System.out.println("Uso: client.ClientLogin <IPADDRESS> <PORT: int>");
      return;
    }
    
    final InetAddress ipAddress;
    try {
      ipAddress = InetAddress.getByName(args[0]);
    } catch (UnknownHostException ex) {
      System.out.println("Direccion IP desconocida");
      return;
    }
    final int servicePort = Integer.parseInt(args[1]);

    final LoginContext lc;
    try {
      lc = new LoginContext("CLIENT");
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
      
      // 3: se ejecuta el codigo de cliente con los permisos del sujeto autenticado
      final ClientTask clientTask = new ClientTask(ipAddress, servicePort);
      Subject.doAsPrivileged(subject, clientTask, null);

      // Se elimina sujeto autenticado y credenciales asociadas
      try {
        lc.logout();
      } catch (final LoginException ex) {
        System.err.println("Fallo al eliminar el contexto de login" + ex.getMessage());
      }
  

    } catch (final LoginException ex) {
      System.err.println("Autenticación fallida en arranque de la aplicación");
    } catch (final SecurityException ex) {
      System.err.println("Usuario no autorizado");
    }

  }

}