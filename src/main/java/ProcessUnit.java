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

        public String[] stringToDataArray(String data){
            String[]dataArray=data.split(",");
            /*
            * index - value
            * 0 sensorName
            * 1 sensorX
            * 2 sensorY
            * 3 sensorXVelocity
            * 4 sensorYVelocity
            * 5 targetName
            * 6 targetX
            * 7 targetY
            * 8 targetXVelocity
            * 9 targetYVelocity
            * */
            return dataArray;
        }
        public void processUnit(String []data){
            if(data.length==10){
                String msg="";
                double difX=Double.parseDouble(data[6])-Double.parseDouble(data[1]);
                double difY=Double.parseDouble(data[7])-Double.parseDouble(data[2]);
                double difXVelocity=Double.parseDouble(data[8])-Double.parseDouble(data[3]);
                double difYVelocity=Double.parseDouble(data[9])-Double.parseDouble(data[4]);
                if(difX==difY && difY==0){
                    msg+="Hedef ve sensor aynı noktada,";
                }else{
                    double angle=(Math.atan2(difX, difY) * (180 / Math.PI));
                    if(angle<=0){
                        angle+=360;
                    }
                    msg+=data[0]+" için "+data[5]+" kerterizi Y pozitif saat yönünde açısı:"+angle+" derecedir.";
                    msg+=data[0]+" için "+data[5]+" bağıl hızı X ekseni için:"+difXVelocity+",Y ekseni için:"+difYVelocity+" değeridir.";
                    System.out.println(msg);
                }
            }else{
                System.out.println("data is wrong");
            }

        }
        @Override
        public void run() {
            try (
                    ObjectInputStream ois = new ObjectInputStream(sensor.getInputStream());
                    ObjectOutputStream oos = new ObjectOutputStream(sensor.getOutputStream())
            ) {
                while (true) {
                    String data = (String) ois.readObject();
                    if (data == "") {
                        oos.writeObject("Process Unit:NULL Data received");
                    } else {
                        oos.writeObject("Process Unit:Data received");
                        String[]dataArr=stringToDataArray(data);
                        processUnit(dataArr);
                    }
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
        readObjectFromXML("src/main/resources/ProcessUnit.xml");
        ProcessUnit processUnit = new ProcessUnit();
        new Thread(() -> {
            processUnit.createServer(port);
        }).start();
    }
}
