import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ProcessUnit {
    private static int port;

    public void createServer(int port) {
        try (ServerSocket server = new ServerSocket(port)) {
            server.setReuseAddress(true);
            while (true) {
                Socket sensor = server.accept();
                sensor.setKeepAlive(true);
                System.out.println("Sensor Connected:" + sensor.getInetAddress().getHostAddress());
                SensorHandler sensorHandler = new SensorHandler(sensor);
                Thread thread = new Thread(sensorHandler);
                thread.start();
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static class SensorHandler implements Runnable {
        Socket sensor;

        public SensorHandler(Socket socket) {
            this.sensor = socket;
        }


        @Override
        public void run() {
            try (
                    ObjectInputStream ois = new ObjectInputStream(sensor.getInputStream());
                    ObjectOutputStream oos = new ObjectOutputStream(sensor.getOutputStream())
            ) {
                while (true) {
                    String data = (String) ois.readObject();
                    oos.writeObject("Process Unit:Data received");
                    System.out.println(data);
                }

            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Target connection is broken");
            }
        }
    }

    public static void readObjectFromXML(String xmlPath) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(xmlPath));
            doc.getDocumentElement().normalize();
            NodeList list = doc.getElementsByTagName("ProcessUnit");

            for (int temp = 0; temp < list.getLength(); temp++) {
                Node node = list.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    port = Integer.parseInt(element.getElementsByTagName("port").item(0).getTextContent());
                }
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) {
        readObjectFromXML("C:\\Users\\stj.eergen\\Desktop\\Processor\\src\\main\\resources\\ProcessUnit.xml");
        ProcessUnit processUnit=new ProcessUnit();
        new Thread(()->{
            processUnit.createServer(port);
        }).start();
    }
}
