import java.io.File;
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
import cgi.cgi_lib;

/** @author Allan Wá
 */
public class Covidgraphspread {
	public static Git git;
	public static Document documento;

	public static void main( String[] args ) throws InvalidRemoteException, TransportException, GitAPIException, IOException{
		criarTabela();
		abrirRepositorio();
		getFicheirosDosTags();

		System.out.println(cgi_lib.Header());
		Hashtable form_data = cgi_lib.ReadParse(System.in);
		System.out.println(documento.select("table"));
		System.out.println(documento.select("style"));
		System.out.println(cgi_lib.HtmlBot());
	}
	/** Entrar no repositorio do gir para exportar os ficheiros .rdf
	 */
	public static void abrirRepositorio() {
		File ficheiro = new File("./ESII1920");
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
			List<Ref> call = git.tagList().call();

			for (Ref ref : call) {
				String tag = ref.getName();
				RevWalk walk = new RevWalk(repositorio);
				try {
					RevObject object = walk.parseAny(ref.getObjectId());

					if (object instanceof RevCommit) {
						encontrarFicheiroNoCommit(ref.getObjectId(), tag);
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

	

	/**Dado um commitI e o tag associado procura pelo ficheiro rdf e se estiver la mete na tabela
	 * @param commitId
	 * @param tag
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws CorruptObjectException
	 * @throws IOException
	 */
	public static void encontrarFicheiroNoCommit(ObjectId commitId, String tag) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		Repository repositorio = git.getRepository();

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
				String[] tags = tag.split("/");
				String finalTag = tags[2];
				adicionarInfoNoHtml(commit, finalTag);
			}
			revWalk.dispose();
		}
	}

	/**Dado um commit cria o link para o rdf
	 * @param commit
	 * @return
	 */
	public static String getHyperlinkDoFicheiroDoCommit(RevCommit commit) {
		String hyperlink = "http://www.visualdataweb.de/webvowl/#iri=https://raw.githubusercontent.com/vbasto-iscte/ESII1920/" + commit.getName() + "/covid19spreading.rdf";
		return hyperlink;
	}

	/**Criar tabela html e os estilos
	 */
	public static void criarTabela() {
		documento = Jsoup.parse("<html></html>");
		documento.body().addClass("body-styles-cls");
		documento.body().appendElement("div");
		documento.body().appendElement("table").attr("id", "t01");
		documento.body().appendElement("style");
		Element tabela = documento.select("table").get(0);
		tabela.append("<tr></tr>");
		Element headersRow = tabela.select("tr").get(0);
		headersRow.append("<th>File timestamp</th>");
		headersRow.append("<th>File name</th>");
		headersRow.append("<th>File tag</th>");
		headersRow.append("<th>Tag Description</th>");
		headersRow.append("<th>Spread Visualization Link</th>");

		Element style = documento.select("style").get(0);
		style.append("table, th, td {border: 5px solid black; border-collapse: collapse;}");
		style.append("th, td {\r\n" + "  padding: 15px;\r\n" + "}");
		style.append("th {\r\n" + 	"  text-align: center;\r\n" + 		"}");
		style.append("table {\r\n" + "  border-spacing: 9px;\r\n" + "}");
	}
	
	/**Transportar a informacao para a tabela html
	 * @param commit
	 * @param tag
	 */
	public static void adicionarInfoNoHtml(RevCommit commit, String tag) {
		Date tempo = commit.getAuthorIdent().getWhen();
		String descricao = commit.getFullMessage();
		String[] rowData = new String[5];
		rowData[0] = tempo.toString();
		rowData[1] = "covid19spreading.rdf";
		rowData[2] = tag;
		rowData[3] = descricao;
		rowData[4] = "<a href='" + getHyperlinkDoFicheiroDoCommit(commit) + "'>Link</a>";

		Element tabela = documento.select("table").get(0);
		Element newRow = tabela.append("<tr></tr>");
		int newRowIndex = tabela.select("tr").size() - 1;
		for(int i =0; i < rowData.length; i++) {
			tabela.select("tr").get(newRowIndex).append("<td>" + rowData[i] + "</td>");
		}
	}
}
