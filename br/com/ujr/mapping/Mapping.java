package br.com.ujr.mapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.SchemaGlobalElement;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.SchemaTypeSystem;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.impl.xsd2inst.SampleXmlUtil;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("rawtypes")
public class Mapping {

	public static Logger		logger	= LoggerFactory.getLogger(Mapping.class);
	private File				schemaFile;
	private String				typesToExtract;
	private Map<String, String>	xmls	= new HashMap<String, String>();
	private Map<String, String>	xPaths	= new HashMap<String, String>();
	private Map<String,Node>    nodes   = new HashMap<String,Node>();

	public static void main(String[] args) {

		// String complexTypes =
		// "PosicaoCorretagemBovespa ListaPosicaoCorretagemBovespa";
		String complexTypes = null;
		String arquivo = "C:/Users/talent.uazambuja/Developer/svn-repo/Servicos/trunk/modules/PosicaoCorretagemBovespaService/docs/SoapUI Project/"
				+ "PosicaoCorretagemBovespa.xsd";

		Mapping mapping = new Mapping(new File(arquivo), complexTypes);
		// System.out.println(mapping.getXPath("ListaPosicaoCorretagemBovespa"));
		//mapping.buildExcel("ListaPosicaoCorretagemBovespa", "c:/temp");
		mapping.buildWord("ListaPosicaoCorretagemBovespa", "c:/temp");
		//mapping.printXMLAllTypes(System.out);
		
		System.out.println("end");
	}

	public Mapping(File file) {
		this(file, null);
	}

	public Mapping(File file, String types) {
		this.schemaFile = file;
		this.typesToExtract = types;
		
		if ( !this.schemaFile.exists() ) {
			throw new RuntimeException("Arquivo não encontrado " + this.schemaFile.getAbsolutePath());
		}
		
		buildXmlAndXPath();
	}

	private void buildXmlAndXPath() {
		try {
			List<XmlObject> listXmlObjects = new ArrayList<XmlObject>();
			listXmlObjects.add(XmlObject.Factory.parse(schemaFile, (new XmlOptions()).setLoadLineNumbers().setLoadMessageDigest()));
			XmlObject[] xmlObjectArray = new XmlObject[listXmlObjects.size()];
			listXmlObjects.toArray(xmlObjectArray);

			XmlOptions compileOptions = new XmlOptions();
			compileOptions.setCompileDownloadUrls();
			compileOptions.setCompileNoPvrRule();
			compileOptions.setCompileNoUpaRule();
			SchemaTypeSystem sts = XmlBeans.compileXsd(xmlObjectArray, XmlBeans.getBuiltinTypeSystem(), compileOptions);

			/**
			 * Process the Elements at XSD
			 */
			SchemaGlobalElement[] schemaGlobalElements = sts.globalElements();
			for(SchemaGlobalElement schemaGlobalElement : schemaGlobalElements) {
				String typeName = schemaGlobalElement.getName().getLocalPart();
				SchemaType schema = schemaGlobalElement.getType();
				processSchema(typeName, schema);
			}
			
			
			/**
			 * Process the ComplexTypes  at XSD
			 */
			SchemaType[] schemas = sts.globalTypes();
			for (SchemaType schema : schemas) {
				String typeName = schema.getName().getLocalPart();
				processSchema(typeName, schema);
			}
		} catch (XmlException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void processSchema(String typeName, SchemaType schema) throws XmlException {
		Pattern patternXmlFragment = Pattern.compile("xml-fragment");
		if (typesToExtract == null || typesToExtract.contains(typeName)) {
			String xmlContent = SampleXmlUtil.createSampleForType(schema);
			xmlContent = patternXmlFragment.matcher(xmlContent).replaceAll(typeName);
			this.xmls.put(typeName, xmlContent);

			XmlObject object = XmlObject.Factory.parse(xmlContent);
			// XmlCursor cursor = object.newCursor();

			Node node = object.getDomNode();
			this.nodes.put(typeName, node);
			NodeList listNode = node.getChildNodes();
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < listNode.getLength(); i++) {
				Node n = listNode.item(i);
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					listNodeAttributes(n.getNodeName(), buffer, (Element) n);
					listNodeChildren(n.getNodeName(), buffer, (Element) n);
				}
			}
			this.xPaths.put(typeName, buffer.toString());
		}
	}

	private void listNodeAttributes(String parentChain, StringBuffer buffer, Element n) {
		NamedNodeMap attrsMap = n.getAttributes();
		for (int i = 0; i < attrsMap.getLength(); i++) {
			Node a = attrsMap.item(i);
			if (!a.getNodeName().contains("xmlns")) {
				buffer.append("/").append(parentChain);
				buffer.append("@").append(cleanNamespaceFromNode(a.getNodeName()));
				buffer.append("\n");
			}
		}

	}

	private void listNodeChildren(String parentChain, StringBuffer buffer, Element elementParent) {
		NodeList childNodeList = elementParent.getChildNodes();

		for (int i = 0; i < childNodeList.getLength(); i++) {
			Node nodeChild = childNodeList.item(i);
			if (nodeChild.getNodeType() == Node.ELEMENT_NODE) {
				Element elementChild = (Element) nodeChild;
				buffer.append("/").append(parentChain);
				buffer.append("/").append(cleanNamespaceFromNode(elementChild)).append("\n");
				if (elementChild.hasChildNodes()) {
					listNodeChildren(parentChain + "/" + cleanNamespaceFromNode(elementChild), buffer, elementChild);
				}
			}
		}
	}

	private String cleanNamespaceFromNode(String str) {
		return str.substring(str.indexOf(":") + 1);
	}

	private String cleanNamespaceFromNode(Element e) {
		return this.cleanNamespaceFromNode(e.getNodeName());
	}

	public void buildExcel(String type, String excelPath) {
		String file = excelPath + (excelPath.endsWith("/") ? "" : "/") + type;
		HSSFWorkbook wb = new HSSFWorkbook();
		// CreationHelper createHelper = wb.getCreationHelper();

		createXPATHSheet(type, wb);
		createXMLSheet(type, wb);

		FileOutputStream fileOut = null;
		try {
			File f = new File(file + ".xls");
			if ( !f.exists() ) {
				f.createNewFile();
			}
			fileOut = new FileOutputStream(f, false);
			wb.write(fileOut);
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} finally {
			try {
				if (fileOut != null) fileOut.close();
			} catch (IOException e) {
			}
		}
	}

	private void createXPATHSheet(String type, HSSFWorkbook wb) {
		String safeName = WorkbookUtil.createSafeSheetName("XPATH");
		Sheet sheet1 = wb.createSheet(safeName);

		int rowIndex = 0;
		int cellIndex = 0;
		Scanner scanner = new Scanner(this.xPaths.get(type));
		while (scanner.hasNextLine()) {
			Row row = sheet1.createRow((short) rowIndex);

			cellIndex = 0;
			Cell cell = row.createCell(cellIndex);
			cell.setCellValue(scanner.nextLine());
			// row.createCell(++cellIndex).setCellValue(1.2);
			++rowIndex;
		}
		scanner.close();
	}

	private void createXMLSheet(String type, HSSFWorkbook wb) {
		String safeName = WorkbookUtil.createSafeSheetName("XML");
		Sheet sheet1 = wb.createSheet(safeName);

		int rowIndex = 0;
		int cellIndex = 0;
		Scanner scanner = new Scanner(this.xmls.get(type));
		while (scanner.hasNextLine()) {
			Row row = sheet1.createRow((short) rowIndex);

			cellIndex = 0;
			Cell cell = row.createCell(cellIndex);
			cell.setCellValue(scanner.nextLine());
			// row.createCell(++cellIndex).setCellValue(1.2);
			++rowIndex;
		}
		scanner.close();
	}
	
	public void buildWord(String type, String wordPath) {
		String file = wordPath + (wordPath.endsWith("/") ? "" : "/") + type;
		XWPFDocument doc = new XWPFDocument();
		int rows = 0;
		Scanner scanner = new Scanner(this.xPaths.get(type));
		while (scanner.hasNextLine()) {scanner.nextLine();rows++;}
		scanner.close();
		
        XWPFTable   tableXPath = doc.createTable(rows + 2, 2);
        CTTbl       tableCtbl  = tableXPath.getCTTbl();
        CTTblPr     tablePr    = tableCtbl.getTblPr();
        CTTblWidth  tableTblW  = tablePr.getTblW();
        CTJc        tableJc    = tablePr.addNewJc();
        tableJc.setVal(STJc.CENTER);
        tablePr.setJc(tableJc);
        tableTblW.setW(BigInteger.valueOf(5000));
        tableTblW.setType(STTblWidth.PCT);
        tablePr.setTblW(tableTblW);
        tableCtbl.setTblPr(tablePr);
        
        // Header
        XWPFTableRow rowTable = tableXPath.getRow(1);
		CTTrPr  trRow = rowTable.getCtRow().addNewTrPr();
		trRow.addNewTrHeight().setVal(BigInteger.valueOf(180));
		
		XWPFTableCell rowCell0 = rowTable.getCell(0);
		CTTcPr tcpr0 = rowCell0.getCTTc().addNewTcPr();
		tcpr0.addNewGridSpan();
		tcpr0.getGridSpan().setVal(BigInteger.valueOf(2));
		tcpr0.addNewVAlign().setVal(STVerticalJc.CENTER);
		tcpr0.addNewShd().setFill("A7BFDE");
		
		
		XWPFParagraph p1 = rowCell0.getParagraphs().get(0);
		p1.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r1 = p1.createRun();
        r1.setText(type);
        r1.setFontSize(10);
        // End Header
        
        int rowIndex = 2;
		scanner = new Scanner(this.xPaths.get(type));
		while (scanner.hasNextLine()) {
			
			String content = scanner.nextLine();
			rowTable = tableXPath.getRow(rowIndex);
			trRow = rowTable.getCtRow().addNewTrPr();
			trRow.addNewTrHeight().setVal(BigInteger.valueOf(180));
			
			rowCell0 = rowTable.getCell(0);
			tcpr0 = rowCell0.getCTTc().addNewTcPr();
			tcpr0.addNewVAlign().setVal(STVerticalJc.CENTER);
			tcpr0.addNewShd().setFill("A7BFDE");
			
			p1 = rowCell0.getParagraphs().get(0);
			p1.setAlignment(ParagraphAlignment.CENTER);
	        r1 = p1.createRun();
	        r1.setText(String.valueOf(rowIndex-1));
	        r1.setFontSize(10);
	        //r1.setItalic(true);
	        //r1.setFontFamily("Courier");
	        //r1.setUnderline(UnderlinePatterns.DOT_DOT_DASH);
	        //r1.setTextPosition(100);
			//tableXPath.getRow(rowIndex).getCell(0).setText();
	        
	        XWPFTableCell rowCell1 = rowTable.getCell(1);
	        CTTcPr tcpr1 = rowCell0.getCTTc().addNewTcPr();
	        tcpr1.addNewVAlign().setVal(STVerticalJc.CENTER);
	        XWPFParagraph p2 = rowCell1.getParagraphs().get(0);
	        XWPFRun r2 = p2.createRun();
	        r2.setText(content);
	        r2.setFontSize(10);
	        
			++rowIndex;
		}
		scanner.close();
        

        FileOutputStream out = null;
		try {
			File f = new File(file + ".docx");
			if ( !f.exists() ) {
				f.createNewFile();
			}
			out = new FileOutputStream(file + ".docx",false);
			doc.write(out);
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(),e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			logger.error(e.getMessage(),e);
			throw new RuntimeException(e);
		} finally {
	        try {
				if ( out != null ) out.close();
			} catch (IOException e) {
			}
		}
        
	}

	public String getXml(String type) {
		return this.xmls.get(type);
	}

	public String getXPath(String type) {
		return this.xPaths.get(type);
	}

	public String[] listAllFoundTypes() {
		List<String> list = new ArrayList<String>();
		for (String type : this.xmls.keySet()) {
			list.add(type);
		}
		Collections.sort(list);
		String[] result = new String[list.size()];
		list.toArray(result);
		return result;
	}

	public String listXMLAllTypes() {
		StringBuffer s = new StringBuffer();
		for (String xml : this.xmls.values()) {
			s.append(xml).append("\n").append("\n");
		}
		return s.toString();
	}

	public void printXMLAllTypes(PrintStream out) {
		out.print(this.listXMLAllTypes());
	}

	public Node getRoot(String complexType) {
		return nodes.get(complexType);
	}
	
	

}
