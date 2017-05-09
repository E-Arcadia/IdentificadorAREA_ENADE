import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.sun.xml.internal.txw2.NamespaceResolver;

/*
 * ABR-2017
 * Autor: prof. Martin Mor�es e prof. Eduardo SIM
 * Descri��o: Identificador de c�digo de �rea do ENADE para os anos anteriores a 2012.
 * 
 *  Os c�digos de �rea dos ENADE de 2012, 2013, 2014 e 2015 s�o os V�LIDOS e s�o a base para definir os c�digos para 
 *  os anos anteriores.
 *  
 *  Processo: 
 *  Cada registro � analisado. 
 *  	Se o c�digo � V�LIDO o registro � guardado
 *  	Se o c�digo n�o � V�LIDO:
 *  		Procura-se se existe no mapa de equival�ncia (relacaoAreaAntigaValidas.csv / encontraAreaValidasEquivalentes)
 *  			Para as �rea equivalentes � procurado se a IES tem ocorr�ncia dessa �rea nos anos de refer�ncia - V�LIDOS
 *  				Se tem ocorr�ncias para a IES s�o gerados NOVOS registros com os c�digos alterados
 *  				Se N�O tem ocorr�ncia para a IES s�o identificados como "N�O IDENTIFICADOS"
 */

public class Principal {
	private String arquivoIMPORTAR = "conceito_enade_2007.csv";
	private String CAMINHO = "C:\\Users\\marti\\Documents\\INEP_indicadores_de_qualidade\\ENADE\\CSV\\";
	private int coluna_IES_IMPORT, coluna_AREA_IMPORT;

	private List<String> listaConvertidaImportar = new ArrayList<>();
	private List<String> listaOriginalImportar;
	private Map<String, String> mapRelacaoIesAreaValidas = new HashMap<String, String>();
	private Map<String, String> mapAreasValidas = new HashMap<String, String>();
	private List<String[]> listaArrayRelacaoAreasAntigas_E_Validas = new ArrayList<>();
	private List<String> listaResultadoLOG = new ArrayList<>();
	private int convertidos = 0, naoIdentificados = 0, aproveitados = 0, linhaVazia = 0;

	public Principal() throws IOException {
		preparaArquivos();
		geraImport();
		escreveArquivo("LOG", listaResultadoLOG);
		escreveArquivo("NOVO", listaConvertidaImportar);
		listarQuantidades();

		// listarMap(mapAreasValidas);
		// listarMap(mapRelacaoIesAreaValidas);
		// listarLista(listaOriginaisNovos_LOG);
		// listarLista(listaOriginalImportar);
	}

	private void geraImport() {
		boolean cabecalho = true;
		for(String linha : listaOriginalImportar){
			String[] campos = linha.split(";");
		
			if (cabecalho) {
				identificaCOLUNAS(campos);
				cabecalho = false;
				registraLOG("ClASSIFICA��O", linha);
			} else {

				if (campos.length > 0)
					if (mapAreasValidas.containsKey(campos[coluna_AREA_IMPORT])) {
						// Area registrada IGUAL area valida
						listaConvertidaImportar.add(linha);
						registraLOG("APROVEITADOS", linha);
						aproveitados++;
					} else {
						// Area registrada DIFERENTE das area valida
						List<String[]> areasEquivalentesEncontradas = encontraAreaValidasEquivalentes(
								campos[coluna_AREA_IMPORT]);

						// Pesquisa se a IES tem ocorr�ncia dessa �rea
						List<String[]> areasEquivalentesParaIES = encontrarIesComArea(campos[coluna_IES_IMPORT],
								areasEquivalentesEncontradas);

						if (!areasEquivalentesParaIES.isEmpty()) {
							// gera registro com a nova �rea
							List<String> listaNovosImportar = geraRegistroComAreaCerta(campos,
									areasEquivalentesParaIES);
							registraLOG("ORIGINAL", linha);
							registraLOG("NOVO", listaNovosImportar);
							listaConvertidaImportar.addAll(listaNovosImportar);
							convertidos++;
						} else {
							// INALTERADOS
							registraLOG("N�O IDENTIFICADOS", linha);
							naoIdentificados++;
						}
					}
				else {
					registraLOG("LINHA VAZIA", linha);
					linhaVazia++;
				}//else linha vazia
			}//else cabe�alho
		}//fecha for
	}//metodo geraImpot

	private void identificaCOLUNAS(String[] campos) {
		Scanner teclado = new Scanner(System.in);
		System.out.println("IDENTIFIQUE AS COLUNAS SOLICITADAS \n==================================");
		for (int x = 0; x <= 8; x++) {
			System.out.println(x + " - " + campos[x]);
		}
		System.out.println("Qual o campo C�DIGO IES:");
		coluna_IES_IMPORT = teclado.nextInt();

		System.out.println("Qual o campo C�DIGO �REA:");
		coluna_AREA_IMPORT = teclado.nextInt();
	}

	private void escreveArquivo(String tipo, List<String> lista) throws IOException {
		String arquivo = arquivoIMPORTAR.split("\\.")[0] + tipo + ".csv";
		Files.write(Paths.get(CAMINHO + arquivo), lista, StandardCharsets.ISO_8859_1);
	}

	private void registraLOG(String classificacao, List<String> listaNovosImportar) {
		listaNovosImportar.forEach(linha -> listaResultadoLOG.add(classificacao + ";" + linha));
	}

	private void registraLOG(String classificacao, String linhaOriginal) {
		listaResultadoLOG.add(classificacao + ";" + linhaOriginal);
	}

	private List<String> geraRegistroComAreaCerta(String[] campos, List<String[]> areasEquivalentesParaIES) {
		List<String> listaNovosImportar = new ArrayList<>();
		areasEquivalentesParaIES.forEach(linha -> {
			String[] novo = campos.clone();
			novo[coluna_AREA_IMPORT] = linha[1];
			listaNovosImportar.add(arrayToCsv(novo));
		});
		return listaNovosImportar;
	}

	private String arrayToCsv(String[] novo) {
		String emCSV = null;
		for (String campo : novo)
			if (emCSV == null)
				emCSV = campo;
			else
				emCSV += ";" + campo;
		// System.out.println(emCSV);
		return emCSV;
	}

	private List<String[]> encontrarIesComArea(String codIES, List<String[]> areasEquivalentes) {
		List<String[]> iesAreasEncontradas = new ArrayList<>();
		areasEquivalentes.forEach(linha -> {
			if (mapRelacaoIesAreaValidas.containsKey(codIES + linha[1])) {
				iesAreasEncontradas.add(linha);
			}
		});
		return iesAreasEncontradas;
	}

	private List<String[]> encontraAreaValidasEquivalentes(String areaPesquisada) {
		List<String[]> areasEncontradas = new ArrayList<>();
		// Localiza area(s) valida(s) equivalente(s)
		listaArrayRelacaoAreasAntigas_E_Validas.forEach(linhaCampoAreasAntigasValidas -> {
			if (linhaCampoAreasAntigasValidas[0].equals(areaPesquisada)) {
				areasEncontradas.add(linhaCampoAreasAntigasValidas);
			}
		});
		return areasEncontradas;
	}

	private void preparaArquivos() throws IOException {
		// ARQUIVO A IMPORTAR
		listaOriginalImportar = Files.readAllLines(Paths.get(CAMINHO + arquivoIMPORTAR), StandardCharsets.ISO_8859_1);

		// RELA��O IES AREA
		List<String> listaRelacaoIES_AREA = Files.readAllLines(Paths.get(CAMINHO + "relacao_IES_AREA.csv"),
				StandardCharsets.ISO_8859_1);
		criaMapRelacaoIesAreaValidas(listaRelacaoIES_AREA);

		// RELA��O DE AREA VALIDAS
		List<String> listaAreasValidas = Files.readAllLines(Paths.get(CAMINHO + "relacaoAreas.csv"),
				StandardCharsets.ISO_8859_1);
		criaMapAreasValidas(listaAreasValidas);

		// RELA��O AREA ANTIGAS E VALIDAS
		List<String> listaRelacaoAreasAntigas_E_Validas = Files
				.readAllLines(Paths.get(CAMINHO + "relacaoAreaAntigaValidas.csv"), StandardCharsets.ISO_8859_1);
		criaListaArrayRelacaoAreasAntigas_E_Validas(listaRelacaoAreasAntigas_E_Validas);
	}

	private void criaListaArrayRelacaoAreasAntigas_E_Validas(List<String> listaRelacaoAreasAntigas_E_Validas) {
		listaRelacaoAreasAntigas_E_Validas.forEach(linhaAreasAntigasValidas -> {
			String[] camposAreasAntigasValidas = linhaAreasAntigasValidas.split(";");
			listaArrayRelacaoAreasAntigas_E_Validas.add(camposAreasAntigasValidas);
		});
	}

	private void criaMapAreasValidas(List<String> listaAreasValidas) {
		listaAreasValidas.forEach(linha -> {
			String[] campos = linha.split(";");
			mapAreasValidas.put(campos[0], linha);
		});
	}

	private void criaMapRelacaoIesAreaValidas(List<String> listaRelacaoIES_AREA) {
		listaRelacaoIES_AREA.forEach(linha -> {
			String[] campos = linha.split(";");
			mapRelacaoIesAreaValidas.put(campos[0] + campos[1], linha);
		});
	}

	private void listarMap(Map<String, String> map) {
		map.forEach((chave, valor) -> {
			System.out.println("Chave: " + chave + " \t\t Valor: " + valor);
		});

	}

	private void listarLista(List<String> lista) {
		lista.forEach(linha -> System.out.println(linha));
	}

	private void listarQuantidades() {
		System.out.println("\n\n" + arquivoIMPORTAR.split("\\.")[0]);
		int total = aproveitados + convertidos + naoIdentificados + 1 + linhaVazia;
		System.out.println("\nCabe�alho: 1\nAproveitados: " + aproveitados + "\nConvertidos: " + convertidos
				+ "\nN�o identificados: " + naoIdentificados + "\nLinhas Vazias: "+ linhaVazia +"\nTOTAL: " + total + "\n");

		System.out.println("Importar ORIGEM: " + listaOriginalImportar.size());
		System.out.println("Importar Validado: " + listaConvertidaImportar.size() + " - UM � cabe�alho.");
		System.out.println("IES e Areas: " + mapRelacaoIesAreaValidas.size());
		System.out.println("Areas V�lidas: " + mapAreasValidas.size());
		System.out.println("Rela��o Areas Antigas e Novas: " + listaArrayRelacaoAreasAntigas_E_Validas.size());
	}

	public static void main(String[] args) throws IOException {
		new Principal();
	}

}
