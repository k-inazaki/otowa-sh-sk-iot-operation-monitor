
//********************************************************************************
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.io.ClientBootstrap;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.crt.io.HostResolver;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.MqttClientConnectionEvents;
import software.amazon.awssdk.crt.mqtt.MqttMessage;
import software.amazon.awssdk.crt.mqtt.QualityOfService;
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import java.nio.charset.StandardCharsets;

//import javax.swing.table.TableColumn;
import java.awt.event.*;
import java.awt.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.BlockingQueue;
import java.text.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
//import java.util.Vector;
//********************************************************************************
class MyRunner extends Thread {
  BlockingQueue<String> queue;
  String msg = "";
  MqttClientConnection connection;
  MqttClientConnectionEvents callbacks;

  MyRunner(BlockingQueue<String> queue) {
    this.queue = queue;
  }

  public void run() {
    System.out.println("非同期的に処理を実行します。");
    open();
    while (msg.indexOf("$stop!") == -1) {
      try {
        //msg = (String) this.queue.take();
        String s = this.queue.poll(1, TimeUnit.SECONDS);
        if (s == null) {
          continue;
        }
        msg = s;
        System.out.printf("MqttClient::request message [%d:%s]\n", msg.length(), msg);
        //
        System.out.println("<1>");
        if (msg.indexOf("$get:status!") != -1) {
          System.out.println("<2>");
          String keepalive = "OTOWA";
          CompletableFuture<Integer> published = connection.publish(
              new MqttMessage(Config.pubtopicKeepAlive, keepalive.getBytes(), QualityOfService.AT_LEAST_ONCE, false));
          System.out.printf("<<<<< CompletableFuture(%d) >>>>>\n", published.get());
          System.out.println("<3>");
          continue;
        }
        System.out.println("<4>");
      } catch (CrtRuntimeException | InterruptedException | ExecutionException ex) {
        ex.printStackTrace();
      }
    }
  }

  // ======================================================================
  public void open() {
    callbacks = new MqttClientConnectionEvents() {
      @Override
      public void onConnectionInterrupted(int errorCode) {
        if (errorCode != 0) {
          System.out.println("Connection interrupted: " + errorCode + ": " + CRT.awsErrorString(errorCode));
        }
      }

      @Override
      public void onConnectionResumed(boolean sessionPresent) {
        System.out.println("Connection resumed: " + (sessionPresent ? "existing session" : "clean session"));
      }
    };
    try {
      System.out.println(Config.certificatePath + Config.rootCa);
      System.out.println(Config.certificatePath + Config.cert);
      System.out.println(Config.certificatePath + Config.key);
      EventLoopGroup eventLoopGroup = new EventLoopGroup(1);
      HostResolver resolver = new HostResolver(eventLoopGroup);
      ClientBootstrap clientBootstrap = new ClientBootstrap(eventLoopGroup, resolver);
      AwsIotMqttConnectionBuilder builder = AwsIotMqttConnectionBuilder
          .newMtlsBuilderFromPath(Config.certificatePath + Config.cert, Config.certificatePath + Config.key);
      if (Config.rootCa != null) {
        builder.withCertificateAuthorityFromPath(null, Config.certificatePath + Config.rootCa);
      }
      builder.withBootstrap(clientBootstrap)
          .withConnectionEventCallbacks(callbacks)
          .withClientId(Config.clientId)
          .withEndpoint(Config.endpoint)
          .withPort((short) Config.port)
          .withCleanSession(true)
          .withProtocolOperationTimeoutMs(60000);
      connection = builder.build();
      CompletableFuture<Boolean> connected = connection.connect();
      try {
        boolean sessionPresent = connected.get();
        System.out.println("Connected to " + (!sessionPresent ? "new" : "existing") + " session!");
      } catch (Exception ex) {
        throw new RuntimeException("Exception occurred during connect", ex);
      }
      CompletableFuture<Integer> subscribed = connection.subscribe(Config.subtopic, QualityOfService.AT_LEAST_ONCE,
          (message) -> {
            subscribedProc(message);
          });
      subscribed.get();
    } catch (CrtRuntimeException | InterruptedException | ExecutionException ex) {
      System.out.println("Exception encountered: " + ex.toString());
    }
  }

  // ======================================================================
  public void subscribedProc(MqttMessage message) {
    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
    System.out.println("Topic: " + message.getTopic() + "\n" + payload);
    /*
     * try {
     * File file = new File(Config.rsp_json);
     * FileWriter filewriter = new FileWriter(file);
     * filewriter.write(payload);
     * filewriter.close();
     * } catch (IOException e) {
     * System.out.println(e);
     * }
     */
  }

  // ======================================================================
  public void close() {
    try {
      CompletableFuture<Void> disconnected = connection.disconnect();
      disconnected.get();
      System.out.println("stop!");
    } catch (CrtRuntimeException | InterruptedException | ExecutionException ex) {
      System.out.println("Exception encountered: " + ex.toString());
    }
  }

}

// ********************************************************************************
@SuppressWarnings("serial")
public class TableTest12 extends JFrame implements ActionListener {
  DefaultTableModel model;
  JButton button;

  Object[][] obj = { null, null, null };
  String[] columnNames = { "No.", "名称", "状態", "", "No.", "名称", "状態", "", "No.", "名称", "状態", "", "No.", "名称", "状態" };
  BlockingQueue<String> queue;

  public TableTest12(BlockingQueue<String> queue) {
    super("免雷盤(IoT)");
    this.queue = queue;
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Font font = new Font("メイリオ", Font.PLAIN, 24);
    setFont(font);
    setSize(1500, 800);

    DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
    JTable table = new JTable(tableModel);
    table.setRowHeight(28);
    // テーブルのフォントを指定
    table.setFont(font);
    // ヘッダーのフォントを指定
    table.getTableHeader().setFont(font);
    for (int i = 0; i < 20; i++) {
      tableModel.addRow(obj);
    }
    for (int i = 0; i < 20; i++) {
      tableModel.setValueAt(i + 1, i, 0);
      tableModel.setValueAt("SPD" + (i + 1), i, 1);
      tableModel.setValueAt("●", i, 2);
    }
    for (int i = 0; i < 20; i++) {
      tableModel.setValueAt(i + 21, i, 4);
      tableModel.setValueAt("SPD" + (i + 21), i, 5);
      tableModel.setValueAt("●", i, 6);
    }
    for (int i = 0; i < 20; i++) {
      tableModel.setValueAt(i + 41, i, 8);
      tableModel.setValueAt("SPD" + (i + 41), i, 9);
      tableModel.setValueAt("●", i, 10);
    }
    for (int i = 0; i < 20; i++) {
      tableModel.setValueAt(i + 61, i, 12);
      tableModel.setValueAt("SPD" + (i + 61), i, 13);
      tableModel.setValueAt("✖", i, 14);
    }
    // 中央揃え
    DefaultTableCellRenderer tableCellRenderer = new DefaultTableCellRenderer();
    tableCellRenderer.setHorizontalAlignment(JLabel.CENTER);
    table.getColumnModel().getColumn(0).setCellRenderer(tableCellRenderer);
    table.getColumnModel().getColumn(1).setCellRenderer(tableCellRenderer);
    table.getColumnModel().getColumn(2).setCellRenderer(tableCellRenderer);
    table.getColumnModel().getColumn(4).setCellRenderer(tableCellRenderer);
    table.getColumnModel().getColumn(5).setCellRenderer(tableCellRenderer);
    table.getColumnModel().getColumn(6).setCellRenderer(tableCellRenderer);
    table.getColumnModel().getColumn(8).setCellRenderer(tableCellRenderer);
    table.getColumnModel().getColumn(9).setCellRenderer(tableCellRenderer);
    table.getColumnModel().getColumn(10).setCellRenderer(tableCellRenderer);
    table.getColumnModel().getColumn(12).setCellRenderer(tableCellRenderer);
    table.getColumnModel().getColumn(13).setCellRenderer(tableCellRenderer);
    table.getColumnModel().getColumn(14).setCellRenderer(tableCellRenderer);
    DefaultTableColumnModel columnModel = (DefaultTableColumnModel) table.getColumnModel();
    columnModel.getColumn(0).setPreferredWidth(30);
    columnModel.getColumn(1).setPreferredWidth(100);
    columnModel.getColumn(2).setPreferredWidth(30);
    columnModel.getColumn(3).setPreferredWidth(10);
    columnModel.getColumn(4).setPreferredWidth(30);
    columnModel.getColumn(5).setPreferredWidth(100);
    columnModel.getColumn(6).setPreferredWidth(30);
    columnModel.getColumn(7).setPreferredWidth(10);
    columnModel.getColumn(8).setPreferredWidth(30);
    columnModel.getColumn(9).setPreferredWidth(100);
    columnModel.getColumn(10).setPreferredWidth(30);
    columnModel.getColumn(11).setPreferredWidth(10);
    columnModel.getColumn(12).setPreferredWidth(30);
    columnModel.getColumn(13).setPreferredWidth(100);
    columnModel.getColumn(14).setPreferredWidth(30);
    JScrollPane scrollPane = new JScrollPane(table);
    button = new JButton("add");
    button.addActionListener(this);
    this.add(button, BorderLayout.SOUTH);

    add(scrollPane);
  }

  public void actionPerformed(ActionEvent e) {
    try {
      queue.put("$get:status!");
    } catch (InterruptedException e2) {
      e2.printStackTrace();
    }
  }

  public static void main(String[] args) {
    BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
    //
    SwingUtilities.invokeLater(new Runnable() {

      @Override
      public void run() {
        TableTest12 app = new TableTest12(queue);
        app.setVisible(true);
      }
    });
    //
    MyRunner myRunner = new MyRunner(queue);
    myRunner.start();
  }
}
// ********************************************************************************
