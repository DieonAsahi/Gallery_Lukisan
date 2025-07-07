/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import object.LanguageManager;
import object.LanguageUpdateListener;
import object.SessionManager;
import object.User;
import object.config;

/**
 *
 * @author athaw
 */
public class gallery_pelukis extends javax.swing.JFrame implements LanguageUpdateListener {

    /**
     * Creates new form Dashboard
     */
    private object.User up;

    public gallery_pelukis() {
        initComponents();
        jScrollPane1.setViewportView(panelLukisan); // Pastikan panel tersambung
        setLocationRelativeTo(null);
        setResizable(false);
        loadData();
        updateLanguage();
        setupUI();
        viewdashboard("");

        this.up = up;
        if (this.up != null) {
            txtfullname.setText(up.getName());
        }
        mulaiAutoRefreshGrafik();
    }
    
    private void setupUI() {
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(jPanel1, BorderLayout.CENTER); // jPanel1 ini harus sudah ada di form designer
    }


    public gallery_pelukis(object.User up) {
        initComponents();
        setLocationRelativeTo(null);
        setResizable(false);
        loadData();
        setupUI();
        updateLanguage();
        viewdashboard("");

        jScrollPane1.setViewportView(panelLukisan);
        setLocationRelativeTo(null);
        setResizable(false);

        this.up = up; // ✅ Simpan user yang dikirim

        if (this.up != null) {
            txtfullname.setText(up.getName());
        }
    }
    
    // Di class gallery_pelukis
public void refresh() {
    loadData();
    updateLanguage();
    viewdashboard("");
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

    @Override
    public void updateLanguage() {
        LanguageManager.init(SessionManager.get().getBahasa()); // Pastikan bahasa sesuai session
        ResourceBundle rb = LanguageManager.getBundle();
        lblselamat.setText(rb.getString("lblselamat.text"));
        lbltgl.setText(rb.getString("lbltgl.text"));
        lbljam.setText(rb.getString("lbljam.text"));
        lblgalery.setText(rb.getString("lblgalery.text"));
        jgallery.setText(rb.getString("jgallery.text"));
        jlogout.setText(rb.getString("jlogout.text"));
        setTitle(rb.getString("Pelukis.title"));
    }

    private void mulaiAutoRefreshGrafik() {
        int delay = 1000; // 10 detik (dalam milidetik)

        Timer timer = new Timer(delay, (e) -> {
            loadData();
            refresh();
        });
        timer.start();
    }

    private void loadData() {
        User currentUser = SessionManager.get();
        panelLukisan.removeAll(); // Kosongkan panel

        // Layout: grid dengan 3 kolom dan jarak antar komponen
        panelLukisan.setLayout(new GridLayout(0, 3, 10, 10));

        try (Connection conn = config.configDB()) {
            String sql = """
            SELECT l.kode_karya, l.judul, l.tahun, l.gambar_path, u.nama 
            FROM lukisan l 
            JOIN users u ON l.user_id = u.id 
            WHERE u.username = ?
        """;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, currentUser.getUsername());
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                String kode = rs.getString("kode_karya");
                String judul = rs.getString("judul");
                String tahun = rs.getString("tahun");
                String pathGambar = rs.getString("gambar_path");
                String namaPelukis = rs.getString("nama");

                System.out.println("Gambar: " + pathGambar);
                System.out.println("Exists? " + new File(pathGambar).exists());

                // Panel 1 karya
                JPanel card = new JPanel();
                card.setLayout(new BorderLayout());
                card.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                card.setPreferredSize(new Dimension(180, 230));
                card.setBackground(Color.LIGHT_GRAY);

                // Gambar
                JLabel lblGambar = new JLabel();
                lblGambar.setHorizontalAlignment(JLabel.CENTER);
                lblGambar.setVerticalAlignment(JLabel.CENTER);

                try {
                    ImageIcon icon = new ImageIcon(pathGambar);
                    Image img = icon.getImage().getScaledInstance(160, 120, Image.SCALE_SMOOTH);
                    lblGambar.setIcon(new ImageIcon(img));
                } catch (Exception e) {
                    lblGambar.setText("Gambar tidak ditemukan");
                }

                // Informasi
                JLabel lblInfo = new JLabel("<html><b>" + judul + "</b><br/>"
                        + "Pelukis: " + namaPelukis + "<br/>"
                        + "Tahun: " + tahun + "<br/>"
                        + "Kode: " + kode + "</html>");
                lblInfo.setHorizontalAlignment(JLabel.CENTER);
                lblInfo.setVerticalAlignment(JLabel.TOP);
                lblInfo.setFont(new Font("Arial", Font.PLAIN, 12));

                card.add(lblGambar, BorderLayout.CENTER);
                card.add(lblInfo, BorderLayout.SOUTH);

                card.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        new edit_lukisan(kode).setVisible(true);
                    }
                });

                panelLukisan.add(card);
            }

            rs.close();
            pst.close();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat data: " + e.getMessage());
        }

        panelLukisan.revalidate();
        panelLukisan.repaint();
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
        jgallery = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        lblgalery = new javax.swing.JLabel();
        jButton5 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        panelLukisan = new javax.swing.JPanel();
        jButton6 = new javax.swing.JButton();

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
                                .addGap(84, 84, 84)
                                .addComponent(txtlevel1))
                            .addGroup(jPanel8Layout.createSequentialGroup()
                                .addGap(135, 135, 135)
                                .addComponent(txtfullname))))
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addGap(133, 133, 133)
                        .addComponent(lblselamat)))
                .addContainerGap(152, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel8Layout.createSequentialGroup()
                .addContainerGap(21, Short.MAX_VALUE)
                .addComponent(lblselamat)
                .addGap(18, 18, 18)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtfullname, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
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
                .addComponent(labelJAM, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                .addComponent(lblDate, javax.swing.GroupLayout.DEFAULT_SIZE, 47, Short.MAX_VALUE))
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
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jlogout, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addGap(0, 4, Short.MAX_VALUE)
                        .addComponent(jlogout, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel18, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );

        jPanel4.setBackground(new java.awt.Color(210, 180, 140));

        jgallery.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jgallery.setText("Gallery");

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/gambar/pngwing.com (6) (1) (1).png"))); // NOI18N
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
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
                        .addComponent(jButton4))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(43, 43, 43)
                        .addComponent(jgallery)))
                .addContainerGap(45, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addComponent(jButton4)
                .addGap(43, 43, 43)
                .addComponent(jgallery)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lblgalery.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        lblgalery.setText("Gallery Lukisan");

        jButton5.setText("Tambah Lukisan");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        panelLukisan.setBackground(new java.awt.Color(100, 100, 100));

        javax.swing.GroupLayout panelLukisanLayout = new javax.swing.GroupLayout(panelLukisan);
        panelLukisan.setLayout(panelLukisanLayout);
        panelLukisanLayout.setHorizontalGroup(
            panelLukisanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1008, Short.MAX_VALUE)
        );
        panelLukisanLayout.setVerticalGroup(
            panelLukisanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 397, Short.MAX_VALUE)
        );

        jScrollPane1.setViewportView(panelLukisan);

        jButton6.setText("Edit Lukisan");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(92, 92, 92)
                .addComponent(lblgalery)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton5)
                .addGap(77, 77, 77)
                .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(106, 106, 106))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1020, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton5)
                            .addComponent(jButton6)))
                    .addComponent(lblgalery))
                .addGap(31, 31, 31)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 393, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, 1020, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
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

        // Periksa pilihan pengguna
        if (response == JOptionPane.YES_OPTION) {
            this.setVisible(false); // Sembunyikan jendela saat ini
            new login().setVisible(true); // Tampilkan halaman login
        }
    }//GEN-LAST:event_jlogoutActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        new tambah_lukisan().setVisible(true);
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        new profil(this).setVisible(true);
    }//GEN-LAST:event_jButton4ActionPerformed

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
            java.util.logging.Logger.getLogger(gallery_pelukis.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(gallery_pelukis.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(gallery_pelukis.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(gallery_pelukis.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new gallery_pelukis().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JLabel jgallery;
    private javax.swing.JButton jlogout;
    private javax.swing.JLabel labelJAM;
    private javax.swing.JLabel lblDate;
    private javax.swing.JLabel lblgalery;
    private javax.swing.JLabel lbljam;
    private javax.swing.JLabel lblselamat;
    private javax.swing.JLabel lbltgl;
    private javax.swing.JPanel panelLukisan;
    private javax.swing.JLabel txtfullname;
    private javax.swing.JLabel txtlevel1;
    // End of variables declaration//GEN-END:variables
}
