package cgi;

import java.util.*;
class hello
{
  public static void main( String args[] )
  {
      //  Here is a minimalistic CGI program that uses cgi_lib
      //  Print the required CGI header.
      System.out.println(cgi_lib.Header());

      //  Parse the form data into a Hashtable.
      Hashtable form_data = cgi_lib.ReadParse(System.in);

      // Create the Top of the returned HTML page
      System.out.println("Here are the name/value pairs from the form:");

      //  Print the name/value pairs sent from the browser.
      System.out.println(cgi_lib.Variables(form_data));

      System.out.println("Nome: " + form_data.get("name"));
      System.out.println("<p>");
      System.out.println("Email: " + form_data.get("email"));      
      System.out.println("<p>");

      System.out.println(cgi_lib.HtmlBot());
  }
}
