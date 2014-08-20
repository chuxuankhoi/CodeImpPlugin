package codeimp.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.internal.core.refactoring.RefactoringCorePlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Restriction: pairs of name and id of all elements are unique
 * 
 * @author khoicx
 * 
 */
@SuppressWarnings("restriction")
public class Configuration {
	
	/** The class attribute */
	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$

	/** The id attribute */
	private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$

	/** The refactoring contributions extension point */
	private static final String REFACTORING_CONTRIBUTIONS_EXTENSION_POINT = "refactoringContributions"; //$NON-NLS-1$

	private static final String URL = "dropins/config.xml";
	
	private static Document doc;

	public static void initialize() {
		File fXmlFile = new File(URL);
		if (!fXmlFile.exists()) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder;
			try {
				dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.newDocument();
				// append root element to document
				Element rootElement = doc.createElement("config");
				doc.appendChild(rootElement);

				// append elements
				createRefactoringsNode(doc, rootElement);
				createMetricsNode(doc, rootElement);

				writeToFile(fXmlFile, doc);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// get configuration data
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();
		} catch (Exception e) {
		}
	}

	private static void writeToFile(File xmlFile, Document doc)
			throws TransformerFactoryConfigurationError,
			TransformerConfigurationException, TransformerException {
		// for output to file
		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		// for pretty print
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		// create source
		DOMSource source = new DOMSource(doc);
		// write to file
		StreamResult file = new StreamResult(xmlFile);
		// write data
		transformer.transform(source, file);
	}

	private static void createMetricsNode(Document doc, Element rootElement) {
		Element metricsElement = doc.createElement("metrics");
		Element metricElement;
		// LCOM2
		{
			metricElement = doc.createElement("metric");
			metricElement.setAttribute("id", "LCOM2");
			Element weightElement = doc.createElement("weight");
			weightElement.setTextContent("1");
			metricElement.appendChild(weightElement);
			Element usedElement = doc.createElement("used");
			usedElement.setTextContent(new Boolean(true).toString());
			metricElement.appendChild(usedElement);
			metricsElement.appendChild(metricElement);
		}
		// LCOM5
		{
			metricElement = doc.createElement("metric");
			metricElement.setAttribute("id", "LCOM5");
			Element weightElement = doc.createElement("weight");
			weightElement.setTextContent("1");
			metricElement.appendChild(weightElement);
			Element usedElement = doc.createElement("used");
			usedElement.setTextContent(new Boolean(true).toString());
			metricElement.appendChild(usedElement);
			metricsElement.appendChild(metricElement);
		}
		// TCC
		{
			metricElement = doc.createElement("metric");
			metricElement.setAttribute("id", "TCC");
			Element weightElement = doc.createElement("weight");
			weightElement.setTextContent("1");
			metricElement.appendChild(weightElement);
			Element usedElement = doc.createElement("used");
			usedElement.setTextContent(new Boolean(true).toString());
			metricElement.appendChild(usedElement);
			metricsElement.appendChild(metricElement);
		}
		// InheritedRatio
		{
			metricElement = doc.createElement("metric");
			metricElement.setAttribute("id", "InheritedRatio");
			Element weightElement = doc.createElement("weight");
			weightElement.setTextContent("1");
			metricElement.appendChild(weightElement);
			Element usedElement = doc.createElement("used");
			usedElement.setTextContent(new Boolean(true).toString());
			metricElement.appendChild(usedElement);
			metricsElement.appendChild(metricElement);
		}
		// SharedMethods
		{
			metricElement = doc.createElement("metric");
			metricElement.setAttribute("id", "SharedMethods");
			Element weightElement = doc.createElement("weight");
			weightElement.setTextContent("1");
			metricElement.appendChild(weightElement);
			Element usedElement = doc.createElement("used");
			usedElement.setTextContent(new Boolean(true).toString());
			metricElement.appendChild(usedElement);
			metricsElement.appendChild(metricElement);
		}
		// SharedMethodsInChildren
		{
			metricElement = doc.createElement("metric");
			metricElement.setAttribute("id", "SharedMethodsInChildren");
			Element weightElement = doc.createElement("weight");
			weightElement.setTextContent("1");
			metricElement.appendChild(weightElement);
			Element usedElement = doc.createElement("used");
			usedElement.setTextContent(new Boolean(true).toString());
			metricElement.appendChild(usedElement);
			metricsElement.appendChild(metricElement);
		}
		// EmptyClass
		{
			metricElement = doc.createElement("metric");
			metricElement.setAttribute("id", "EmptyClass");
			Element weightElement = doc.createElement("weight");
			weightElement.setTextContent("1");
			metricElement.appendChild(weightElement);
			Element usedElement = doc.createElement("used");
			usedElement.setTextContent(new Boolean(true).toString());
			metricElement.appendChild(usedElement);
			metricsElement.appendChild(metricElement);
		}
		// LongMethod
		{
			metricElement = doc.createElement("metric");
			metricElement.setAttribute("id", "LongMethod");
			Element weightElement = doc.createElement("weight");
			weightElement.setTextContent("1");
			metricElement.appendChild(weightElement);
			Element usedElement = doc.createElement("used");
			usedElement.setTextContent(new Boolean(true).toString());
			metricElement.appendChild(usedElement);
			metricsElement.appendChild(metricElement);
		}

		rootElement.appendChild(metricsElement);
	}

	private static void createRefactoringsNode(Document doc, Element rootElement) {
		Element refactoringsElements = doc.createElement("refactorings");
		final IConfigurationElement[] elements = Platform
				.getExtensionRegistry().getConfigurationElementsFor(
						RefactoringCore.ID_PLUGIN,
						REFACTORING_CONTRIBUTIONS_EXTENSION_POINT);
		for (int index = 0; index < elements.length; index++) {
			final IConfigurationElement element = elements[index];
			final String attributeId = element.getAttribute(ATTRIBUTE_ID);
			if (attributeId != null && !"".equals(attributeId)) { //$NON-NLS-1$
				final String className = element.getAttribute(ATTRIBUTE_CLASS);
				if (className != null && !"".equals(className)) { //$NON-NLS-1$
					try {
						final Object implementation = element
								.createExecutableExtension(ATTRIBUTE_CLASS);
						if (implementation instanceof RefactoringContribution) {
							createRefactoringElement(doc, refactoringsElements,
									attributeId, false);
						}
					} catch (CoreException exception) {
						RefactoringCorePlugin.log(exception);
					}
				}
			}
		}
		rootElement.appendChild(refactoringsElements);
	}

	private static void createRefactoringElement(Document document,
			Element parent, String id, boolean isCustom, String... additions) {
		Element refactoring = document.createElement("refactoring");
		refactoring.setAttribute("id", id);
		Element isCustomElement = document.createElement("isCustom");
		isCustomElement.setTextContent(new Boolean(isCustom).toString());
		refactoring.appendChild(isCustomElement);
		Element additionElement = document.createElement("addition");
		for (int i = 0; i < additions.length; i++) {
			Element elem = document.createElement(additions[i]);
			elem.setAttribute("id", "" + i);
			additionElement.appendChild(elem);
		}
		refactoring.appendChild(additionElement);
		parent.appendChild(refactoring);
	}

	/**
	 * Get the value of a element in specific element
	 * 
	 * @param parentType
	 *            name of the parent of node
	 * @param id
	 *            unique string representing the element which is the parent of
	 *            node
	 * @param nodeName
	 *            name of node (child element) we need
	 * @return the first value of element named infoName (in string format)
	 */
	public static String getInfoInElement(String parentType, String id,
			String nodeName) {
		Element parent = findElement(parentType, id);
		if (parent == null) {
			return "";
		}
		Node node = parent.getElementsByTagName(nodeName).item(0);
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			return node.getTextContent();
		}
		return "";
	}

	/**
	 * Add an element to a specific element
	 * 
	 * @param parentType
	 *            name of the parent of node
	 * @param id
	 *            the representation of the parent element
	 * @param name
	 *            name of new element
	 * @param value
	 *            value of new element (in String format)
	 */
	public static void addElement(String parentType, String id, String name,
			String value) {
		if(doc == null) {
			return;
		}
		Element parent = findElement(parentType, id);
		if(parent == null) {
			return;
		}
		Element child = doc.createElement(name);
		child.setTextContent(value);
		parent.appendChild(child);
	}

	public static void removeElement(String name, String id) {
		if(doc == null) {
			return;
		}
		Element element = findElement(name, id);
		if(element == null) {
			return;
		}
		Node parent = element.getParentNode();
		parent.removeChild(element);
	}

	/**
	 * 
	 * @param parentType
	 *            name of the parent of element
	 * @param name
	 *            name of element
	 * @return
	 */
	public static String[] getIDsOfTagName(String parentType, String id,
			String name) {
		ArrayList<String> idsList = new ArrayList<String>();

		Element parent = findElement(parentType, id);
		if (parent == null) {
			return null;
		}
		NodeList nodes = parent.getElementsByTagName(name);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				idsList.add(((Element) node).getAttribute("id"));
			}
		}

		String[] ids = new String[idsList.size()];
		ids = idsList.toArray(ids);
		return ids;
	}

	private static Element findElement(String name, String id) {
		if (doc == null) {
			return null;
		}
		NodeList nodes = doc.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeName().equals(name)) {
				if (!id.equals("")) {
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						if (((Element) node).getAttribute("id").equals(id)) {
							return (Element) node;
						}
					}
				} else {
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						return (Element) node;
					}
				}
			}
			Element e = findElementInNode(node, name, id);
			if (e != null) {
				return e;
			}
		}
		return null;
	}

	private static Element findElementInNode(Node node, String name, String id) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeName().equals(name)) {
				if (!id.equals("")) {
					if (child.getNodeType() == Node.ELEMENT_NODE) {
						if (((Element) child).getAttribute("id").equals(id)) {
							return (Element) child;
						}
					}
				} else {
					if (child.getNodeType() == Node.ELEMENT_NODE) {
						return (Element) child;
					}
				}
			}
			Element e = findElementInNode(child, name, id);
			if (e != null) {
				return e;
			}
		}
		return null;
	}

	public static void updateElementInfo(String elementName, String elementID,
			String... children) {
		if (children.length % 2 != 0) {
			return;
		}
		Element element = findElement(elementName, elementID);
		NodeList elemChildren = element.getChildNodes();
		for (int i = 0; i < children.length - 1; i += 2) {
			String childName = children[i];
			String childInfo = children[i + 1];
			for (int j = 0; i < elemChildren.getLength(); j++) {
				Node node = elemChildren.item(j);
				if (node.getNodeName().equals(childName)) {
					node.setTextContent(childInfo);
					break;
				}
			}
		}
	}

	public static void saveChanged() {
		File fXmlFile = new File(URL);
		if (doc != null) {
			try {
				writeToFile(fXmlFile, doc);
			} catch (TransformerFactoryConfigurationError
					| TransformerException e) {
				e.printStackTrace();
			}
		}
	}

	public static void getInfo(HashMap<String, String> map,
			String elementName, String keyName, String valueName) {
		if(doc == null) {
			return;
		}
		NodeList nodes = doc.getElementsByTagName(elementName);
		for(int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String key = "";
				if(keyName.equals("id")) {
					key = element.getAttribute("id");
				} else {
					key = element.getElementsByTagName(keyName).item(0).getTextContent();
				}
				String value = "";
				if(valueName.equals("id")) {
					value = element.getAttribute("id");
				} else {
					value = element.getElementsByTagName(valueName).item(0).getTextContent();
				}
				map.put(key, value);
			}
				
		}
	}

	public static boolean isRefactoringDefault(String action) {
		String strIsCustom = getInfoInElement("refactoring", action, "isCustom");
		if(strIsCustom == null || strIsCustom.equals("true")) {
			return false;
		} else {
			return true;
		}
	}
}
