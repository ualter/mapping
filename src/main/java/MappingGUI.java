import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import br.com.ujr.mapping.Mapping;
import br.com.ujr.mapping.Utils;

@SuppressWarnings("serial")
public class MappingGUI extends JDialog implements WindowListener, ListSelectionListener {

	private JFileChooser					fileChooser;
	private JList<String>					listXmlObjects;
	private DefaultListModel<String>		listModelXmlObjects;
	private Panel							panelLoadingXsdFile		= new Panel();
	private JEditorPane						editorPaneXmlContent;
	private JEditorPane						editorPaneXPathContent;
	private JTree							treeXml;
	private final ActionOpenFile			actionOpenFile			= new ActionOpenFile("Load XSD or WSDL", this);
	private final ActionCreateExcelDocument	actionCreateExcelFile	= new ActionCreateExcelDocument();
	private final ActionCreateWordDocument	actionCreateWordFile	= new ActionCreateWordDocument();
	private DefaultTreeModel				treeXmlModel;
	private DefaultMutableTreeNode			rootNodeTreeXml;
	private JLabel							lblLoadedFile;

	private File							loadedXsd;
	private String							selectedComplexType;
	private Mapping							mapping;
	private JButton							btnWord;
	private JButton							btnExcel;

	public MappingGUI() {
		setModalityType(ModalityType.APPLICATION_MODAL);
		setResizable(false);
		setBounds(new Rectangle(5, 5, 1270, 800));

		this.fileChooser = new JFileChooser();

		setTitle("Mapping");
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(null);

		JButton btnOpenXsdFile = new JButton();
		btnOpenXsdFile.setBounds(20, 23, 181, 30);
		getContentPane().add(btnOpenXsdFile);
		btnOpenXsdFile.setText("Load Schema (XSD)");
		btnOpenXsdFile.setAction(actionOpenFile);
		listModelXmlObjects = new DefaultListModel<String>();

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(20, 64, 300, 464);
		getContentPane().add(scrollPane);

		listXmlObjects = new JList<String>();
		scrollPane.setViewportView(listXmlObjects);
		listXmlObjects.setModel(listModelXmlObjects);
		listXmlObjects.addListSelectionListener(this);

		FlowLayout flowLayout = (FlowLayout) panelLoadingXsdFile.getLayout();
		flowLayout.setVgap(100);
		panelLoadingXsdFile.setBackground(Color.WHITE);
		panelLoadingXsdFile.setForeground(Color.BLACK);
		panelLoadingXsdFile.setBounds(22, 65, 295, 463);
		ImageIcon icon = new ImageIcon(this.getClass().getClassLoader().getResource("gears.gif"));
		JLabel lblLoading = new JLabel(icon);
		panelLoadingXsdFile.add(lblLoading);
		getContentPane().add(panelLoadingXsdFile);
		getContentPane().setComponentZOrder(panelLoadingXsdFile, 1);

		JScrollPane scrollPaneXmlContent = new JScrollPane();
		scrollPaneXmlContent.setBounds(330, 63, 559, 324);
		getContentPane().add(scrollPaneXmlContent);

		editorPaneXmlContent = new JEditorPane();
		editorPaneXmlContent.setEditable(false);
		editorPaneXmlContent.setFont(new Font("Courier New", Font.PLAIN, 12));
		editorPaneXmlContent.addMouseListener(new MouseListenerXmlXPath());
		scrollPaneXmlContent.setViewportView(editorPaneXmlContent);
		editorPaneXmlContent.setContentType("text/xml");

		JScrollPane scrollPaneXPath = new JScrollPane();
		scrollPaneXPath.setBounds(330, 417, 919, 334);
		getContentPane().add(scrollPaneXPath);

		editorPaneXPathContent = new JEditorPane();
		editorPaneXPathContent.setFont(new Font("Courier New", Font.PLAIN, 12));
		editorPaneXPathContent.setEditable(false);
		editorPaneXPathContent.addMouseListener(new MouseListenerXmlXPath());
		scrollPaneXPath.setViewportView(editorPaneXPathContent);

		JScrollPane scrollPaneTreeXml = new JScrollPane();
		scrollPaneTreeXml.setBounds(899, 64, 350, 323);
		getContentPane().add(scrollPaneTreeXml);

		rootNodeTreeXml = new DefaultMutableTreeNode();
		this.treeXmlModel = new DefaultTreeModel(rootNodeTreeXml);
		treeXml = new JTree(this.treeXmlModel);
		scrollPaneTreeXml.setViewportView(treeXml);

		lblLoadedFile = new JLabel("");
		lblLoadedFile.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblLoadedFile.setBounds(211, 30, 1043, 14);
		getContentPane().add(lblLoadedFile);

		btnWord = new JButton("Word");
		btnWord.setBounds(20, 539, 141, 35);
		btnWord.setAction(actionCreateWordFile);
		btnWord.setEnabled(false);
		getContentPane().add(btnWord);

		btnExcel = new JButton("Excel");
		btnExcel.setBounds(179, 539, 141, 35);
		btnExcel.setAction(actionCreateExcelFile);
		btnExcel.setEnabled(false);
		getContentPane().add(btnExcel);

		ImageIcon iconExit = new ImageIcon(this.getClass().getClassLoader().getResource("exit.png"));
		JButton btnExit = new JButton("Exit");
		btnExit.setBounds(20, 704, 79, 47);
		getContentPane().add(btnExit);
		btnExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		panelLoadingXsdFile.setVisible(false);

		this.addWindowListener(this);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				MappingGUI gui = null;
				try {
					gui = new MappingGUI();
					gui.setVisible(true);
				} catch (Throwable e) {
					if (gui != null) {
						showQuickErrorDialog(gui, e);
					}
					e.printStackTrace();
				}
			}
		});
	}

	public static void showQuickErrorDialog(Component parent, Throwable e) {
		// create and configure a text area - fill it with exception text.
		final JTextArea textArea = new JTextArea();
		textArea.setFont(new Font("Sans-Serif", Font.PLAIN, 12));
		textArea.setEditable(false);
		StringWriter writer = new StringWriter();
		e.printStackTrace(new PrintWriter(writer));
		textArea.setText(writer.toString());

		// stuff it in a scrollpane with a controlled size.
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setPreferredSize(new Dimension(800, 600));

		// pass the scrollpane to the joptionpane.
		JOptionPane.showMessageDialog(parent, scrollPane, "An Error Has Occurred", JOptionPane.ERROR_MESSAGE);
	}

	private final class MouseListenerXmlXPath implements MouseListener {
		public void mouseClicked(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			if (e.getSource() instanceof JEditorPane) {
				((JEditorPane) e.getSource()).selectAll();
			}
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent oe) {
		}

		public void mouseExited(MouseEvent e) {
		}
	}

	private class ActionOpenFile extends AbstractAction {
		private Component	parent;

		public ActionOpenFile(String label, Component parent) {
			this.parent = parent;
			putValue(NAME, label);
		}

		public void actionPerformed(ActionEvent e) {
			fileChooser.setFileFilter(new FileFilter() {

				@Override
				public String getDescription() {
					return "XSD, WSDL";
				}

				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					if ("xsd".equalsIgnoreCase(Utils.getExtension(f))) {
						return true;
					}
					if ("wsdl".equalsIgnoreCase(Utils.getExtension(f))) {
						return true;
					}
					return false;
				}
			});
			fileChooser.setCurrentDirectory(new File(Utils.getLastAccessFolderForFile()));
			int returnVal = fileChooser.showOpenDialog(parent);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				Utils.saveLastAccessFolderForFile(fileChooser.getSelectedFile().getPath());
				lblLoadedFile.setText(fileChooser.getSelectedFile().getPath());

				if (fileChooser.getSelectedFile().getName().contains(".wsdl")) {
					loadedXsd = createXSDTemporaryFileFromWSDL();
				} else {
					loadedXsd = fileChooser.getSelectedFile();
				}
				btnExcel.setEnabled(true);
				btnWord.setEnabled(true);
				
				LoadFileXSDWorker task = new LoadFileXSDWorker(parent);
				task.execute();
			} else {
				refreshScreen();
			}
		}

		private File createXSDTemporaryFileFromWSDL() {
			File xsdWsdl = null;
			File wsdl = fileChooser.getSelectedFile();
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(wsdl);
				byte[] data = new byte[(int) wsdl.length()];
				fis.read(data);
				String str = new String(data, "UTF-8");

				Matcher matcherNamespaces = Pattern.compile("<wsdl:definitions[\\s\\S]*?>").matcher(str);
				matcherNamespaces.find();
				String listNamespaces[] = str.substring(matcherNamespaces.start(), matcherNamespaces.end()).split(" ");
				String namespaces = "";
				for (String n : listNamespaces) {
					if (!n.contains("wsdl:definitions")) {
						namespaces += " " + n.replaceAll(">", "");
					}
				}

				Pattern patternCompile = Pattern.compile("<wsdl:types>([\\s\\S]*?)<\\/wsdl:types>");
				Matcher matcher = patternCompile.matcher(str);
				if (matcher.find()) {
					String wsdlTypesStr = str.substring(matcher.start(), matcher.end());
					wsdlTypesStr = Pattern.compile("<wsdl:types>").matcher(wsdlTypesStr).replaceAll("");
					wsdlTypesStr = Pattern.compile("<\\/wsdl:types>").matcher(wsdlTypesStr).replaceAll("");
					wsdlTypesStr = wsdlTypesStr.replaceAll("<s:schema ", "<s:schema " + namespaces + " ");
					wsdlTypesStr = Pattern.compile("targetNamespace=\"[\\s\\S]*?\"").matcher(wsdlTypesStr).replaceFirst("");

					xsdWsdl = File.createTempFile(wsdl.getName(), ".xsd");
					FileWriter fw = new FileWriter(xsdWsdl);
					fw.write(wsdlTypesStr);
					fw.flush();
					fw.close();
					
				}
			} catch (FileNotFoundException e1) {
				throw new RuntimeException(e1);
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			} finally {
				if (fis != null) try {
					fis.close();
				} catch (IOException e1) {
				}
			}
			return xsdWsdl;
		}
	}

	private void refreshScreen() {
		btnExcel.setEnabled(false);
		btnWord.setEnabled(false);
		this.lblLoadedFile.setText("");
		this.editorPaneXmlContent.setText("");
		this.editorPaneXPathContent.setText("");
		loadedXsd = null;
	}

	private class ActionCreateExcelDocument extends AbstractAction {
		public ActionCreateExcelDocument() {
			putValue(NAME, "Excel");
		}

		public void actionPerformed(ActionEvent e) {
			String path = MappingGUI.class.getClassLoader().getResource("//").getPath();
			mapping.buildExcel(selectedComplexType, path);
			File f = new File(path + "/" + selectedComplexType + ".xls");
			try {
				Desktop.getDesktop().browse(f.toURI());
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	private class ActionCreateWordDocument extends AbstractAction {
		public ActionCreateWordDocument() {
			putValue(NAME, "Word");
		}

		public void actionPerformed(ActionEvent e) {
			String path = MappingGUI.class.getClassLoader().getResource("//").getPath();
			mapping.buildWord(selectedComplexType, path);
			File f = new File(path + "/" + selectedComplexType + ".docx");
			try {
				Desktop.getDesktop().browse(f.toURI());
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	private class LoadFileXSDWorker extends SwingWorker<String, String> {

		private Component	componentParent;

		public LoadFileXSDWorker(Component componentParent) {
			this.componentParent = componentParent;
			panelLoadingXsdFile.setVisible(true);
		}

		@Override
		protected String doInBackground() throws Exception {
			listModelXmlObjects.clear();
			if (loadedXsd == null) {
				throw new RuntimeException("Nenhum arquivo XSD foi carregado");
			}
			String ret = "OK";
			try {
				mapping = new Mapping(loadedXsd);
				for (String s : mapping.listAllFoundTypes()) {
					listModelXmlObjects.addElement(s);
				}
				ret = "NOK";
			} catch (Throwable e) {
				MappingGUI.showQuickErrorDialog(this.componentParent, e);
				e.printStackTrace();
				throw new RuntimeException(e);
			} finally {
				panelLoadingXsdFile.setVisible(false);
			}
			return ret;
		}

		@Override
		protected void done() {
			super.done();
			panelLoadingXsdFile.setVisible(false);
		}

	}

	public void windowOpened(WindowEvent e) {
	}

	public void windowClosing(WindowEvent e) {
		Utils.saveState();
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	@SuppressWarnings("rawtypes")
	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource() instanceof JList) {
			JList list = (JList) e.getSource();
			String complexType = list.getSelectedValue().toString();
			this.selectedComplexType = complexType;
			this.ploatContentLoadedXsd();
		}
	}

	private void ploatContentLoadedXsd() {
		String xml = this.mapping.getXml(this.selectedComplexType);
		String xpath = this.mapping.getXPath(this.selectedComplexType);
		this.editorPaneXmlContent.setText(xml);
		this.editorPaneXPathContent.setText(xpath);

		Node node = this.mapping.getRoot(this.selectedComplexType);
		NodeList listNode = node.getChildNodes();
		for (int i = 0; i < listNode.getLength(); i++) {
			Node n = listNode.item(i);
			DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(n.getNodeName());
			rootNodeTreeXml.add(treeNode);
		}

	}
}
