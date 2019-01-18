/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dossierbox.cathegory;

/**
 *
 * @author burdas
 */
public class QueryMain {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        BrowserQuery browserQuery = new BrowserQuery("ligeti");
        final String salida = browserQuery.run();
        System.out.println(salida);
    }
    
    
}
