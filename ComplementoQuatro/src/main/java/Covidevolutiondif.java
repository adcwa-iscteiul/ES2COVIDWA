import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import cgi.cgi_lib;


/** @author Allan Wá
 */
public class Covidevolutiondif {
	public static Git git;
	public static Document documento;
	public static List<String> diferencas = new ArrayList<String>();
	public static List<String> ficheiros = new ArrayList<String>();
	public static List<Date> datas = new ArrayList<Date>();
	public static String ficheiro1;
	public static String ficheiro2;

	public static void main( String[] args ) throws InvalidRemoteException, TransportException, GitAPIException, IOException{
		criarHtml();
		abrirRepositorio();
		getFicheirosDosTags();
		encontrarDiferencas();
		try {
			escreverTextoNaArea();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(cgi_lib.Header());
		Hashtable form_data = cgi_lib.ReadParse(System.in);
		System.out.println(documento.select("#ta01"));
		System.out.println(documento.select("#ta02"));
		System.out.println(documento.select("style"));
		System.out.println(cgi_lib.HtmlBot());
	}
	
	/**Acesso ao git para exportar os rdf files
	 */
	public static void abrirRepositorio() {
		File ficheiro = new File("./ESII1920");//path/to/repo
		if (ficheiro.exists() && ficheiro.isDirectory()) {
			try {
				FileUtils.cleanDirectory(ficheiro);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		try {
			git = Git.cloneRepository()
					.setURI("https://github.com/vbasto-iscte/ESII1920")
					//.setDirectory(new File("/Es/Files"))
					.call();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**Lista todos os tags do reposirotio e procura pelos ficheiros associados a eles
	 */
	public static void getFicheirosDosTags() {
		Repository repositorio = git.getRepository();
		try {
			List<Ref> call = git.tagList().call();//as tags
			for (Ref ref : call) {
				RevWalk walk = new RevWalk(repositorio);//para andar no repositorio
				String tag = ref.getName();//leva o nome da tag
				try {
					RevObject object = walk.parseAny(ref.getObjectId());// passar para um RevObject
					if (object instanceof RevCommit) {
						encontrarFicheiroNoCommit(ref.getObjectId(), tag);//dado um commitId e o seu tag
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	


	/**Cria um html divs e adiciona estilo
	 * Creates html divs and adds style to them
	 */
	public static void criarHtml() {
		documento = Jsoup.parse("<html></html>");
		documento.body().addClass("body-styles-cls");
		documento.body().appendElement("div").attr("id", "ta01").attr("class", "split right");
		documento.body().appendElement("div").attr("id", "ta02").attr("class", "split left");
		documento.body().appendElement("style");

		Element estilo = documento.select("style").get(0);
		estilo.append("textarea {width: 48%; height: 98%;}.split {\r\n" + "	height: 98%;\r\n" + "	width: 48%;\r\n" + 
				"	position: fixed;\r\n" + "	z-index: 1;\r\n" + "	top: 0;\r\n" + "	overflow-x: hidden;\r\n" +
				"	padding-top: 18px;\r\n" + "}\r\n" + " \r\n" + ".left {\r\n" + "	left: 0;\r\n" +  "	text-align: center;\r\n" + 
				"}\r\n" + " \r\n" + ".right {\r\n" + "	right: 0;\r\n" + "	text-align: center;\r\n" + "}");
	}

	/**Ve se o str esta na lista de differencas
	 * @param str
	 * @return
	 */
	public static boolean naLista(String str) {
		for(int i=0; i!=diferencas.size(); i++) {
			if(str.contentEquals(diferencas.get(i))) {
				return true;
			}
		}
		return false;
	}
	
	/**Encontra os dois ultimos rdf files
	 */
	public static void ultimosDoisFicheiros() {		
		Date maisrecente = datas.get(0);
		Date menosrecente = datas.get(0);

		for(int i=0; i!=datas.size(); i++) {
			if(datas.get(i).after(maisrecente)) {
				maisrecente = datas.get(i);
				ficheiro1 = ficheiros.get(i);				
			}			
		}
		for(int i=0; i!=datas.size(); i++) {
			if(datas.get(i).before(maisrecente)) {
				menosrecente = datas.get(i);
				ficheiro2 = ficheiros.get(i);
			}			
		}
	}

	/**Escreve cada ficcheiro comparado no div lado a lado
	 * Deixa destacado as linhas q sao differentes num ficheiro
	 * @throws IOException
	 */
	public static void escreverTextoNaArea() throws IOException {
		Elements div1 = documento.select("#ta01");
		Elements div2 = documento.select("#ta02");

		File myObj = new File("TestesGeracaoII.txt");
		Scanner myReader = new Scanner(myObj);
		while (myReader.hasNextLine()) {
			String data = myReader.nextLine();

			String str = data.replaceAll("<", "&lt;");
			str = str.replaceAll(">", "&gt;");
			if(naLista(str)) {
				div1 .append("<mark>"+str+"</mark><br>");
			} else {
				div1 .append(str+"<br>");
			}
		}
		myReader.close();

		File myObj2 = new File("TestesGeracaoIII.txt");
		Scanner myReader2 = new Scanner(myObj2);
		while (myReader2.hasNextLine()) {
			String data = myReader2.nextLine();
			String str = data.replaceAll("<", "&lt;");
			str = str.replaceAll(">", "&gt;");

			if(naLista(str)) {
				div2 .append("<mark>"+str+"</mark><br>");
			} else {
				div2 .append(str+"<br>");
			}
		}
		myReader.close();
	}

	/**Encontra a diferenca entre os dois ficheiros e salva na lista das diferencas
	 * @throws IOException
	 */
	public static void encontrarDiferencas() throws IOException {
		ultimosDoisFicheiros();

		BufferedReader br = null;
		BufferedReader br1 = null;
		BufferedReader br2 = null;
		String sCurrentLine;
		List<String> lista1 = new ArrayList<String>();
		List<String> lista2 = new ArrayList<String>();
		br1 = new BufferedReader(new FileReader(ficheiro1));
		br2 = new BufferedReader(new FileReader(ficheiro2));
		while ((sCurrentLine = br1.readLine()) != null) {
			lista1.add(sCurrentLine);
		}
		while ((sCurrentLine = br2.readLine()) != null) {
			lista2.add(sCurrentLine);
		}
		List<String> tmpList = new ArrayList<String>(lista1);
		tmpList.removeAll(lista2);
		for(int i=0;i<tmpList.size();i++){
			String str = tmpList.get(i).replaceAll("<", "&lt;");
			str = str.replaceAll(">", "&gt;");
			diferencas.add(str);
		}
	}
	
	/** Dado um commitI e o tag associado procura pelo ficheiro rdf e se estiver la mete na tabela
	 * @param commitId
	 * @param tag
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws CorruptObjectException
	 * @throws IOException
	 */
	public static void encontrarFicheiroNoCommit(ObjectId commitId, String tag) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		Repository repositorio = git.getRepository();
		String[] tags = tag.split("/");
		String tagFinal = tags[2];

		try(RevWalk revWalk = new RevWalk(repositorio)){
			RevCommit commit = revWalk.parseCommit(commitId);

			RevTree tree = commit.getTree();
			try(TreeWalk treeWalk = new TreeWalk(repositorio)){
				treeWalk.addTree(tree);
				treeWalk.setRecursive(true);
				treeWalk.setFilter(PathFilter.create("covid19spreading.rdf"));
				if(!treeWalk.next()) {
					throw new IllegalStateException("Didnt find file");
				}
				ObjectId objectId = treeWalk.getObjectId(0);
				ObjectLoader loader = repositorio.open(objectId);
				Date time = commit.getAuthorIdent().getWhen();
				String fileName = time.toString();
				FileOutputStream fos = new FileOutputStream(tagFinal+".txt");
				loader.copyTo(fos);

				datas.add(time);
				ficheiros.add(tagFinal+".txt");
			}
			revWalk.dispose();
		}
	}
	
	
}
