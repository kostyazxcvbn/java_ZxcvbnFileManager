package controllers;/**
 * Created by kostyazxcvbn on 09.07.2017.
 */

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController extends Application {

    private static Stage primaryStage;
    private static Stage currentStage;
    private static ExecutorService threadLogicUIPool;
    private static Document devStringsXml;

    private Locale locale;
    private static ResourceBundle resourceBundle;


    public static String getStringValue(String valueId) {
        return devStringsXml.getElementById(valueId).getTextContent();
    }

    public static ExecutorService getThreadLogicUIPool() {
        return threadLogicUIPool;
    }

    public static void setThreadLogicUIPool(ExecutorService threadLogicUIPool) {
        if (MainController.threadLogicUIPool == null) {
            MainController.threadLogicUIPool = threadLogicUIPool;
        }
    }


    public static ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    public static Stage getCurrentStage() {
        return currentStage;
    }

    public static void setCurrentStage(Stage currentStage) {
        MainController.currentStage = currentStage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void setPrimaryStage(Stage primaryStage) {
        if(MainController.primaryStage==null){
            MainController.primaryStage = primaryStage;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        locale=new Locale("ru");
        resourceBundle=ResourceBundle.getBundle("bundles.locale", locale);

        Schema schema = null;
        DocumentBuilder documentBuilder = null;
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            schema = schemaFactory.newSchema(new File(getClass().getResource("/strings/xsd_app_strings.xsd").getFile()));
        } catch (SAXException e) {
            e.printStackTrace();
        }

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setSchema(schema);

        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            devStringsXml = documentBuilder.parse(new File(getClass().getResource("/strings/dev_strings.xml").getFile()));

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
        e.printStackTrace();
        } catch (IOException e) {
        e.printStackTrace();
        }
        Stage startStage = new Stage(StageStyle.UNDECORATED);;
        setPrimaryStage(primaryStage);
        setThreadLogicUIPool(Executors.newCachedThreadPool());
        FXMLLoader fxmlLoader=null;

        Parent root = null;
        try {
            fxmlLoader = new FXMLLoader(getClass().getResource(getStringValue("fxmlStartScreen")));
            fxmlLoader.setResources(resourceBundle);
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Scene scene=new Scene(root);
        startStage.setScene(scene);
        startStage.sizeToScene();
        startStage.show();
        setCurrentStage(startStage);
        StartScreenController c=fxmlLoader.getController();
        c.init();
    }
}
