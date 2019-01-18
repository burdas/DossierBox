package dossierbox.cathegory;

/**
 *
 * BrowserQuery: esta clase NO esta completamente definida. Hay que definir el
 * metodo publico run(). Modificaciones a realizar en el método privado query():
 * - Debe devolver ambito y nivel de acceso del principal. - Cuando el principal
 * no se encuentre en la base de datos, debe devolver algun tipo de indicación
 * especial.
 *
 * @coauthor MAZ
 */
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;

public final class BrowserQuery implements PrivilegedAction<String> {

    static private final String CLASS_NAME = BrowserQuery.class.getName();
    static private final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    static private final String DB_URL = "jdbc:mysql://eim-srv-myinfclinica.lab.unavarra.es:3306/PS_SV_DB";
    static private final String DB_USER = "DossierBox";
    static private final String DB_PASSWORD = "PS1819";

    private final byte[] idCode;

    public BrowserQuery(final String clientPrincipalName) {
        byte[] bytes = null;
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-1");
            bytes = md.digest(clientPrincipalName.getBytes());
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.log(Level.SEVERE, "No se ha encontrado el algoritmo MessageDigest");
        }
        this.idCode = bytes;
    }

    @Override
    public String run() {
        try {
            final String salida = query(idCode).toString();
//            final String salida = query2().toString();
            return salida;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Problema en la consulta de base de datos");
            LOGGER.severe(ex.getMessage());
            return null;
        }
    }

    private ArrayList<String> query(final byte[] idCode) throws SQLTimeoutException, SQLException {

        try (final Connection connection
                = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            final String selectStatement
                    = "SELECT Scope FROM cathegories WHERE IdCode = ?";

            try (final PreparedStatement statement = connection.prepareStatement(selectStatement)) {

                statement.setBytes(1, idCode);

                final ResultSet rs = statement.executeQuery();
                final ArrayList<String> result = new ArrayList<>();
                while (rs.next()) {
                    String scope = rs.getString("Scope").trim();
                    if (scope.equals("A")) {
                        result.add(scope);
                    } else {
                        result.add(scope + rs.getString("Level").trim());
                    }
                }
                return result;

            } catch (final SQLException ex) {
                LOGGER.info("error al realizar consulta");
                LOGGER.severe(ex.getMessage());
                throw ex;
            }

        } catch (final SQLTimeoutException ex) {
            LOGGER.info("timeout al abrir la conexion");
            LOGGER.severe(ex.getMessage());
            throw ex;
        } catch (final SQLException ex) {
            LOGGER.info("error al abrir la conexion");
            LOGGER.severe(ex.getMessage());
            throw ex;
        }

    }

    private ArrayList<String> query2() throws SQLTimeoutException, SQLException {

        try (final Connection connection
                = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            final String selectStatement
                    = "SELECT * FROM cathegories";

            try (final PreparedStatement statement = connection.prepareStatement(selectStatement)) {

                final ResultSet rs = statement.executeQuery();
                final ArrayList<String> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(rs.toString());
                }
                return result;

            } catch (final SQLException ex) {
                LOGGER.info("error al realizar consulta");
                LOGGER.severe(ex.getMessage());
                throw ex;
            }

        } catch (final SQLTimeoutException ex) {
            LOGGER.info("timeout al abrir la conexion");
            LOGGER.severe(ex.getMessage());
            throw ex;
        } catch (final SQLException ex) {
            LOGGER.info("error al abrir la conexion");
            LOGGER.severe(ex.getMessage());
            throw ex;
        }

    }

}
