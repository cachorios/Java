package applet;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import netscape.javascript.JSObject;


import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Created with IntelliJ IDEA.
 * User: cacho
 * Date: 22/04/13
 * Time: 17:54
 * To change this template use File | Settings | File Templates.
 */
public class Main extends JApplet {
    private static JApplet applet;
    private SerialPort pserie;
    private String nombrePuerto;
    private JSObject js;

    private int baudRate = SerialPort.BAUDRATE_9600;
    private int dataBits = SerialPort.DATABITS_8;
    private int stopBits = SerialPort.STOPBITS_1;
    private int parity = SerialPort.PARITY_NONE;

    public static final int INFO = 0;
    public static final int WARNING = 1;
    public static final int SEVERO = 2;

    private String estado;
    private String okCallback;

    static JApplet getApplet() {
        return applet;
    }


    @Override
    public void init() {
        try {
            try {
                js = JSObject.getWindow(this);
            } catch (netscape.javascript.JSException e) {

            }

            leerParametros();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.out.println("puerto " + nombrePuerto);
                    pserie = new SerialPort(nombrePuerto);
                    applet = Main.this;

                    open();
                }
            });
        } catch (Exception e) {
            System.out.println("No pudo iniciar");
            call("noticia", new Object[]{e.getMessage(), SEVERO});
        }
    }

    public void open() {
        try {

            if (pserie.openPort()) {
                this.printMsg("Se abrio el puerto " + nombrePuerto, 0);
                if (pserie.setParams(baudRate, dataBits, stopBits, parity)) {
                    this.printMsg("Se activo los nuevos parametros del puerto.", INFO);

                   // int mask = SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR;//Prepare mask
                    //pserie.setEventsMask(mask);//Set mask
                  ///  pserie.addEventListener(new SerialPortReader());//Add SerialPortEventListener


                    pserie.addEventListener(new SerialPortReader(), SerialPort.MASK_RXCHAR |
                            SerialPort.MASK_RXFLAG |
                            SerialPort.MASK_CTS |
                            SerialPort.MASK_DSR |
                            SerialPort.MASK_RLSD);
                } else {
                    this.printMsg("No se puede activar estos parametros.", SEVERO);
                    pserie.closePort();
                }
            } else {
                this.printMsg("No se pudo abrir el puerto " + nombrePuerto + ", puede que este en uso. ", SEVERO);

            }
        } catch (SerialPortException e) {
            e.printStackTrace();
            call("noticia", new Object[]{e.getMessage(), SEVERO});
        } catch (Exception e1) {
            if (e1 instanceof InvocationTargetException) {
                System.err.println(((InvocationTargetException) e1).getTargetException().getMessage());
            } else {
                System.out.println(e1.getMessage());
            }
            call("noticia", new Object[]{e1.getMessage(), SEVERO});
        }
    }

    /**
     *
     */
    public void leerParametros() {
        String valor;
        try {
            System.out.println("Leyendo parametro");

            nombrePuerto = getParameter("puerto");
            if (nombrePuerto == null) {
                nombrePuerto = "No definido";
            }
            System.out.println("Puerto: " + nombrePuerto);

            valor = getParameter("baudRate");
            if (valor == null) {
                baudRate = SerialPort.BAUDRATE_9600;
            } else {
                baudRate = Integer.parseInt(valor);
            }
            System.out.printf("BaudRate: %d%n", baudRate);

            valor = getParameter("dataBits");
            if (valor == null) {
                dataBits = SerialPort.DATABITS_8;
            } else {
                dataBits = Integer.parseInt(valor);
            }
            System.out.printf("dataBits: %d%n", dataBits);

            valor = getParameter("stopBits");
            if (valor == null) {
                stopBits = SerialPort.STOPBITS_1;
            } else {
                stopBits = Integer.parseInt(valor);
            }
            System.out.printf("stopBits: %d%n", stopBits);

            valor = getParameter("parity");
            if (valor == null) {
                parity = SerialPort.PARITY_NONE;
            } else {
                parity = Integer.parseInt(valor);
            }
            System.out.printf("parity: %d%n", parity);


        } catch (Exception e) {

            nombrePuerto = "Sin puerto";
            System.out.println(e.getMessage());
            call("noticia", new Object[]{e.getMessage()});
        }


        System.out.println("Parametro leido: " + nombrePuerto);
    }

    /**
     * @param msg
     */
    public void escribir(String msg) {
        if (msg.length() > 0) {
            try {
                pserie.writeBytes(msg.getBytes());
            } catch (Exception ex) {
                System.err.println(ex.getMessage() + " - Ha habido un error minetas se escribia un string");
                call("noticia", new Object[]{ex.getMessage() + " - Ha habido un error mintras se escribia un string"});
            }
        }
    }


    public String getMsg(String nombre) {
        return "Hola " + nombre;
    }

    public void getEstado() {
        //String str = (new StringBuffer()).append((char) 29).append((char) 5).toString();
        byte buffer[];
        buffer = getHex2Bin("1D 05");

        try {
            pserie.writeBytes(buffer);
        } catch (Exception ex) {
            System.err.println(ex.getMessage() + " - Ha habido un error minetas se escribia un string");
            call("noticia", new Object[]{ex.getMessage() + " - Ha habido un error mintras se escribia un string"});
        }
    }


    public void ifEstadoRun(String fCallfunc){
        okCallback = fCallfunc;

        getEstado();
    }

    private void printMsg(String msg, int tipo) {
        if (tipo == 0 || tipo == 2)
            System.out.println(msg);
        if (tipo == 1 || tipo == 2){
            System.err.println(msg);
        }

        call("noticia", new Object[]{msg, tipo});

    }

    private void call(String metodo, Object[] parm) {
        if (js != null)
            js.call(metodo, parm);
    }


    private class SerialPortReader implements SerialPortEventListener {

        public void serialEvent(SerialPortEvent event) {

            if (event.isRXCHAR() || event.isRXFLAG()) {//If data is available
                if (event.getEventValue() >0) {
                    //Read data, if 10 bytes available
                    try {
                        byte buffer[] = pserie.readBytes(event.getEventValue());
                        printMsg("Serila Port Event - Leyendo ", 0);

                        int[] intBuffer = new int[buffer.length];
                        for (int i = 0; i < buffer.length; i++) {
                            if (buffer[i] < 0) {
                                intBuffer[i] = 256 + buffer[i];
                            } else {
                                intBuffer[i] = buffer[i];
                            }
                        }
                        estado = "";
                        for (int i : intBuffer) {
                            estado += (i + " ");
                        }
                        printMsg("Estado " + estado, 0);

                    } catch (SerialPortException ex) {
                        System.out.println(ex);
                    }
                }
            } else if (event.isCTS()) {//If CTS line has changed state
                if (event.getEventValue() == 1) {//If line is ON
                    System.out.println("CTS - ON");
                } else {
                    System.out.println("CTS - OFF");
                }
            } else if (event.isDSR()) {///If DSR line has changed state
                if (event.getEventValue() == 1) {//If line is ON
                    System.out.println("DSR - ON");
                } else {
                    System.out.println("DSR - OFF");
                }
            }

            verfificarEstado();
        }
    }

    private void verfificarEstado(){
        try {
            String msg = "";
            int intEstado;
            if(  (estado != null && estado.length()>0 ) ){
                estado = estado.trim();
                printMsg("Estado " + estado, 0);
                intEstado = Integer.parseInt(estado);
                if( (intEstado & 1)  == 1){
                    msg += ", Testigo sin papel.";
                }
                if( (intEstado  & 2) == 2){
                    msg += ", Ticket sin papel.";
                }
                if( (intEstado  & 4) == 4){
                    msg += ", Puerta Abierta.";
                }

                if( (intEstado & 8) == 8){
                    msg += ", la Impresora no esta lista.";
                }

                if( (intEstado & 64) == 64){
                    msg += ", ERROR EN LA IMPRESORA!!";
                }
                estado = null;
                if(msg.length()>0){
                    this.printMsg(msg.substring(2) ,1);
                }else{
                    //aqui esta todo bien!!
                    if(okCallback != null && okCallback.length()>0 ){
                        call(okCallback, null);
                        okCallback = null;
                    }
                }

            }else{
                if( !(pserie.isCTS() && pserie.isDSR()) )
                    this.printMsg("La impresora esta apagada o ausente.",1);
            }
        } catch (SerialPortException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    private byte[] getHex2Bin(String str) {
        String[] strArray = str.split(" ");
        int[] intArray = new int[strArray.length];

        for (int i = 0; i < strArray.length; i++) {
            intArray[i] = Integer.valueOf(strArray[i], 16);
        }
        byte[] buffer = new byte[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            buffer[i] = (byte) intArray[i];
        }

        return buffer;
    }

    private class Reader implements SerialPortEventListener {
        private String str = "";

        public void serialEvent(SerialPortEvent spe) {
            if (spe.isRXCHAR() || spe.isRXFLAG()) {
                if (spe.getEventValue() > 0) {
                    try {
                        str = "";
                        byte[] buffer = pserie.readBytes(spe.getEventValue());


                        //
                        int[] intBuffer = new int[buffer.length];

                        for (int i = 0; i < buffer.length; i++) {
                            if (buffer[i] < 0) {
                                intBuffer[i] = 256 + buffer[i];
                            } else {
                                intBuffer[i] = buffer[i];
                            }
                        }
                        for (int i : intBuffer) {
//                                if(jComboBoxIn.getSelectedIndex() == 2){
//                                    String value = Integer.toHexString(i).toUpperCase();
//                                    if(value.length() == 1) {
//                                        value = "0" + value;
//                                    }
//                                    str += (value + " ");
//                                }
//                                else {
                            str += (i + " ");
//                                }
                        }
                        printMsg(str, 2);
//                        }

//                        SwingUtilities.invokeAndWait(
//                                new Runnable() {
//                                    public void run() {
//                                        jTextAreaIn.append(str);
//                                        int scrollValue = jScrollPaneIn.getVerticalScrollBar().getValue();
//                                        int scrollBottom = jScrollPaneIn.getVerticalScrollBar().getMaximum() - jScrollPaneIn.getVerticalScrollBar().getVisibleAmount();
//                                        if((scrollValue == scrollBottom) && (scrollValue > 0)){
//                                            jTextAreaIn.setCaretPosition(jTextAreaIn.getText().length());
//                                        }
//                                    }
//                                }
//                        );
                    } catch (Exception ex) {
                        //Do nothing
                    }
                }
            }
//            else if(spe.isCTS()){
//                if(spe.getEventValue() == 1){
//                    jLabelCTS.setBorder(NimbusGui.borderStatusOn);
//                    jLabelCTS.setBackground(NimbusGui.colorStatusOnBG);
//                }
//                else {
//                    jLabelCTS.setBorder(NimbusGui.borderStatusOff);
//                    jLabelCTS.setBackground(NimbusGui.colorStatusOffBG);
//                }
//            }
//            else if(spe.isDSR()){
//                if(spe.getEventValue() == 1){
//                    jLabelDSR.setBorder(NimbusGui.borderStatusOn);
//                    jLabelDSR.setBackground(NimbusGui.colorStatusOnBG);
//                }
//                else {
//                    jLabelDSR.setBorder(NimbusGui.borderStatusOff);
//                    jLabelDSR.setBackground(NimbusGui.colorStatusOffBG);
//                }
//            }
//            else if(spe.isRLSD()){
//                if(spe.getEventValue() == 1){
//                    jLabelRLSD.setBorder(NimbusGui.borderStatusOn);
//                    jLabelRLSD.setBackground(NimbusGui.colorStatusOnBG);
//                }
//                else {
//                    jLabelRLSD.setBorder(NimbusGui.borderStatusOff);
//                    jLabelRLSD.setBackground(NimbusGui.colorStatusOffBG);
//                }
//            }
        }
    }

}


