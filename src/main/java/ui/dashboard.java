package ui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.PlotOrientation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import object.SessionManager;
import object.User;
import object.config;
import object.LanguageManager;
import object.LanguageUpdateListener;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;

/**
 *
 * @author athaw
 */
public class dashboard extends javax.swing.JFrame implements LanguageUpdateListener {

    private object.User up;

    // Constructor tanpa user
    public dashboard() {
        initComponents();
        setupUI();
        viewdashboard(""); // jam dan tanggal tetep jalan
        viewTblPengunjung(""); // jam dan tanggal tetep jalan
        tampilkanGrafikKunjungan("");
        mulaiAutoRefreshGrafik();
        viewTblLukisan();
        // Di constructor setelah komponen diinisialisasi
    }

    // Constructor dengan user
    public dashboard(object.User up) {
        initComponents();
        viewTblPengunjung("");
        tampilkanGrafikKunjungan("");
        mulaiAutoRefreshGrafik();
        setupUI();
        viewTblLukisan();
        viewdashboard("");
        // Di constructor setelah komponen diinisialisasi
        updateLanguage();
        this.up = up;
        if (this.up != null) {
            txtfullname.setText(up.getName());
        }
        
        setLocationRelativeTo(null);
        setResizable(false);

    }

    @Override
    public void updateLanguage() {
        LanguageManager.init(SessionManager.get().getBahasa()); // Pastikan bahasa sesuai session
        ResourceBundle rb = LanguageManager.getBundle();

        lblselamat.setText(rb.getString("lblselamat.text"));
        lbldashboard.setText(rb.getString("lbldashboard.text"));
        lbltgl.setText(rb.getString("lbltgl.text"));
        lbljam.setText(rb.getString("lbljam.text"));
        jlogout.setText(rb.getString("jlogout.text"));

        setTitle(rb.getString("Dasbor.title"));
    }

    private void setupUI() {
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(jPanel1, BorderLayout.CENTER);
    }

    private Map<String, Map<String, Integer>> getKunjunganPerHari() {
        Map<String, Map<String, Integer>> data = new LinkedHashMap<>();

        // Buat tanggal dari 30 hari terakhir
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -29);

        for (int i = 0; i < 30; i++) {
            String tanggal = sdf.format(cal.getTime());
            Map<String, Integer> defaultMap = new HashMap<>();
            defaultMap.put("pengunjung", 0);
            defaultMap.put("pelukis", 0);
            data.put(tanggal, defaultMap);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        String query = """
        SELECT DATE(k.waktu) AS tanggal, u.role, COUNT(*) AS jumlah
        FROM kunjungan k
        JOIN users u ON k.user_id = u.id
        WHERE u.role IN ('pengunjung', 'pelukis') AND k.waktu >= CURDATE() - INTERVAL 30 DAY
        GROUP BY tanggal, u.role
    """;

        try (Connection conn = config.configDB(); PreparedStatement pst = conn.prepareStatement(query); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                String tanggal = new SimpleDateFormat("dd-MM").format(rs.getDate("tanggal"));
                String role = rs.getString("role");
                int jumlah = rs.getInt("jumlah");

                if (data.containsKey(tanggal)) {
                    data.get(tanggal).put(role, jumlah);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mengambil data grafik: " + e.getMessage());
        }

        return data;
    }

    private void mulaiAutoRefreshGrafik() {
        int delay = 1000; // 10 detik (dalam milidetik)

        Timer timer = new Timer(delay, (e) -> {
            tampilkanGrafikKunjungan(""); // refresh grafik
            viewTblPengunjung("");
            //updateLanguage();
        });
        timer.start();
    }

    public void viewdashboard(String where) {
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");

        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); // Tunggu 1 detik
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break; // Keluar dari loop kalau thread dihentikan
                }

                String formattedTime = sdfTime.format(new java.util.Date());
                String formattedDate = sdfDate.format(new java.util.Date());

                // Update komponen GUI harus dilakukan di Event Dispatch Thread
                SwingUtilities.invokeLater(() -> {
                    labelJAM.setText(formattedTime);
                    lblDate.setText(formattedDate);
                });
            }
        });

        thread.setDaemon(true); // Agar thread otomatis berhenti saat aplikasi ditutup
        thread.start();
    }

    private void viewTblPengunjung(String where) {
        String[] kolom = {"Nama", "Role", "Jumlah Kunjungan"};
        DefaultTableModel model = new DefaultTableModel(null, kolom);

        String query = """
        SELECT u.nama, u.role, COUNT(k.id) AS jumlah_kunjungan
        FROM kunjungan k
        JOIN users u ON k.user_id = u.id
        WHERE u.role IN ('pengunjung', 'pelukis')
        GROUP BY k.user_id, u.nama, u.role
        ORDER BY jumlah_kunjungan DESC
    """;

        try (Connection conn = config.configDB(); PreparedStatement stmt = conn.prepareStatement(query); ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String nama = rs.getString("nama");
                String role = rs.getString("role");
                int jumlah = rs.getInt("jumlah_kunjungan");
                model.addRow(new Object[]{nama, role, jumlah});
            }

            tabelpengunjung.setModel(model);

        } catch (Exception ex) {
            System.err.println("Gagal update data kunjungan: " + ex.getMessage());
        }
    }

    private void tampilkanGrafikKunjungan(String where) {
        Map<String, Map<String, Integer>> data = getKunjunganPerHari();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Map.Entry<String, Map<String, Integer>> entry : data.entrySet()) {
            String tanggal = entry.getKey();
            Map<String, Integer> roleMap = entry.getValue();

            dataset.addValue(roleMap.getOrDefault("pengunjung", 0), "Pengunjung", tanggal);
            dataset.addValue(roleMap.getOrDefault("pelukis", 0), "Pelukis", tanggal);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                //        JFreeChart chart = ChartFactory.createLineChart(
                "Grafik Kunjungan Pengunjung dan Pelukis (30 Hari Terakhir)",
                "Tanggal",
                "Jumlah Kunjungan",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        // Buat label tanggal miring biar muat
        CategoryPlot plot = chart.getCategoryPlot();

// Label tanggal miring
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 4));

// 🔧 Format angka di sumbu Y jadi bilangan bulat
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(jPanelGrafik.getWidth(), jPanelGrafik.getHeight()));
        jPanelGrafik.removeAll();
        jPanelGrafik.setLayout(new BorderLayout());
        jPanelGrafik.add(chartPanel, BorderLayout.CENTER);
        jPanelGrafik.revalidate();
        jPanelGrafik.repaint();
    }

    private void viewTblLukisan() {
        String[] kolom = {"Nama Pelukis", "Jumlah Lukisan"};
        DefaultTableModel model = new DefaultTableModel(null, kolom);

        String query = """
        SELECT u.nama AS nama_pelukis, COUNT(l.id) AS jumlah_lukisan
        FROM lukisan l
        JOIN users u ON l.user_id = u.id
        GROUP BY u.nama
        ORDER BY jumlah_lukisan DESC
    """;

        try (Connection conn = config.configDB(); PreparedStatement stmt = conn.prepareStatement(query); ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String namaPelukis = rs.getString("nama_pelukis");
                int jumlah = rs.getInt("jumlah_lukisan");

                model.addRow(new Object[]{namaPelukis, jumlah});
            }

            tabelLukisan.setModel(model); // Sesuaikan dengan nama komponen JTable-mu
        } catch (Exception ex) {
            System.err.println("Gagal menampilkan data jumlah lukisan: " + ex.getMessage());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        txtlevel1 = new javax.swing.JLabel();
        txtfullname = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        lblselamat = new javax.swing.JLabel();
        jPanel18 = new javax.swing.JPanel();
        lbljam = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        labelJAM = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        lbltgl = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        lblDate = new javax.swing.JLabel();
        jlogout = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        lbldashboard = new javax.swing.JLabel();
        profil = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tabelpengunjung = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        tabelLukisan = new javax.swing.JTable();
        jScrollPane4 = new javax.swing.JScrollPane();
        jPanelGrafik = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(100, 100, 100));

        jPanel7.setBackground(new java.awt.Color(105, 105, 105));
        jPanel7.setPreferredSize(new java.awt.Dimension(982, 150));

        jPanel8.setBackground(new java.awt.Color(125, 125, 125));
        jPanel8.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        txtlevel1.setFont(new java.awt.Font("Imprint MT Shadow", 0, 14)); // NOI18N

        txtfullname.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        txtfullname.setText("nama");

        lblselamat.setFont(new java.awt.Font("Segoe UI Emoji", 1, 18)); // NOI18N
        lblselamat.setText("WELCOME!");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addComponent(jLabel8)
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel8Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtlevel1))
                            .addGroup(jPanel8Layout.createSequentialGroup()
                                .addGap(136, 136, 136)
                                .addComponent(txtfullname))))
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addGap(133, 133, 133)
                        .addComponent(lblselamat)))
                .addContainerGap(152, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lblselamat)
                .addGap(18, 18, 18)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtfullname, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(txtlevel1)
                .addGap(24, 24, 24))
        );

        jPanel18.setBackground(new java.awt.Color(125, 125, 125));

        lbljam.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lbljam.setForeground(new java.awt.Color(255, 255, 255));
        lbljam.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbljam.setText("JAM");

        jSeparator5.setBackground(new java.awt.Color(255, 255, 255));
        jSeparator5.setForeground(new java.awt.Color(255, 255, 255));

        labelJAM.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        labelJAM.setForeground(new java.awt.Color(255, 255, 255));
        labelJAM.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelJAM.setText("HH/mm/ss");
        labelJAM.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255), 2));

        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbljam, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jSeparator5, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(labelJAM, javax.swing.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
        );
        jPanel18Layout.setVerticalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbljam)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelJAM, javax.swing.GroupLayout.DEFAULT_SIZE, 47, Short.MAX_VALUE))
        );

        jPanel9.setBackground(new java.awt.Color(125, 125, 125));

        lbltgl.setBackground(new java.awt.Color(125, 125, 125));
        lbltgl.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lbltgl.setForeground(new java.awt.Color(255, 255, 255));
        lbltgl.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbltgl.setText("TANGGAL HARI INI");

        jSeparator4.setBackground(new java.awt.Color(255, 255, 255));
        jSeparator4.setForeground(new java.awt.Color(255, 255, 255));

        lblDate.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblDate.setForeground(new java.awt.Color(255, 255, 255));
        lblDate.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblDate.setText("DD/MM/YYYY");
        lblDate.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255), 2));

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbltgl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jSeparator4, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(lblDate, javax.swing.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbltgl)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblDate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jlogout.setBackground(new java.awt.Color(255, 0, 0));
        jlogout.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jlogout.setForeground(new java.awt.Color(255, 255, 255));
        jlogout.setText("LOGOUT ->");
        jlogout.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jlogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jlogoutActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jlogout, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(19, 19, 19))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jlogout, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel18, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(13, 13, 13))
        );

        jPanel4.setBackground(new java.awt.Color(210, 180, 140));

        lbldashboard.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        lbldashboard.setText("DASHBOARD");

        profil.setIcon(new javax.swing.ImageIcon(getClass().getResource("/gambar/pngwing.com (6) (1) (1).png"))); // NOI18N
        profil.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                profilActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(52, 52, 52)
                        .addComponent(profil))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lbldashboard)))
                .addContainerGap(14, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addComponent(profil)
                .addGap(43, 43, 43)
                .addComponent(lbldashboard)
                .addContainerGap(585, Short.MAX_VALUE))
        );

        tabelpengunjung.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Nama Pengunjung", "Role", "Jumlah Kunjungan"
            }
        ));
        jScrollPane2.setViewportView(tabelpengunjung);
        if (tabelpengunjung.getColumnModel().getColumnCount() > 0) {
            tabelpengunjung.getColumnModel().getColumn(1).setResizable(false);
        }

        tabelLukisan.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Nama Pelukis", "Jumlah Lukisan"
            }
        ));
        jScrollPane3.setViewportView(tabelLukisan);

        jPanelGrafik.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanelGrafikLayout = new javax.swing.GroupLayout(jPanelGrafik);
        jPanelGrafik.setLayout(jPanelGrafikLayout);
        jPanelGrafikLayout.setHorizontalGroup(
            jPanelGrafikLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1102, Short.MAX_VALUE)
        );
        jPanelGrafikLayout.setVerticalGroup(
            jPanelGrafikLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 429, Short.MAX_VALUE)
        );

        jScrollPane4.setViewportView(jPanelGrafik);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 1096, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 444, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 492, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, 1096, Short.MAX_VALUE))
                .addGap(14, 14, 14))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 231, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 231, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 339, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jlogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jlogoutActionPerformed
        // TODO add your handling code here:
        int response = JOptionPane.showConfirmDialog(
                this,
                "Apakah Anda yakin akan keluar?",
                "Konfirmasi Keluar",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        SessionManager.clear();

        // Periksa pilihan pengguna
        if (response == JOptionPane.YES_OPTION) {
            this.setVisible(false); // Sembunyikan jendela saat ini
            new login().setVisible(true); // Tampilkan halaman login
        }
    }//GEN-LAST:event_jlogoutActionPerformed

    private void profilActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_profilActionPerformed
        new profil(this).setVisible(true);
    }//GEN-LAST:event_profilActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(dashboard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(dashboard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(dashboard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(dashboard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new dashboard().setVisible(true);
                } catch (Exception ex) {
                    Logger.getLogger(dashboard.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JPanel jPanelGrafik;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JButton jlogout;
    private javax.swing.JLabel labelJAM;
    private javax.swing.JLabel lblDate;
    private javax.swing.JLabel lbldashboard;
    private javax.swing.JLabel lbljam;
    private javax.swing.JLabel lblselamat;
    private javax.swing.JLabel lbltgl;
    private javax.swing.JButton profil;
    private static javax.swing.JTable tabelLukisan;
    private static javax.swing.JTable tabelpengunjung;
    private javax.swing.JLabel txtfullname;
    private javax.swing.JLabel txtlevel1;
    // End of variables declaration//GEN-END:variables
}
