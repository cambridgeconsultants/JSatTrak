/*
 * JTrackingPanel.java
 * =====================================================================
 * Copyright (C) 2008 Shawn E. Gano
 * 
 * This file is part of JSatTrak.
 * 
 * JSatTrak is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JSatTrak is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JSatTrak.  If not, see <http://www.gnu.org/licenses/>.
 * =====================================================================
 *
 * Created on December 16, 2007, 6:38 PM
 */

package jsattrak.gui;

import jsattrak.objects.GroundStation;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Hashtable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import jsattrak.objects.AbstractSatellite;
import name.gano.astro.AER;
import name.gano.astro.AstroConst;
import name.gano.astro.MathUtils;
import name.gano.astro.bodies.Sun;
import name.gano.astro.time.Time;

/**
 *
 * @author  Shawn
 */
public class JTrackingPanel extends javax.swing.JPanel
{
    
    Hashtable<String,AbstractSatellite> satHash;
    Hashtable<String,GroundStation> gsHash;
    
    // data to tell if Lead/Lag data should be updated
    double oldLeadX = -1;
    double oldLagX = -1;
    
    String timeAsString;
    
    // current time object - passed for use in pass predictions
    Time currentJulianDate;
        
    // table 
    DefaultTableModel passTableModel;
    
    // app
    JSatTrak app;
    
    // hash table to store pass num and midpoint times -- used to setting time in app
    private Hashtable<Integer,Double> passHash = new Hashtable<Integer,Double>();
    
    Double nanDbl = new Double(Double.NaN);
          
    
    /** Creates new form JTrackingPanel
     * @param satHash
     * @param gsHash
     * @param timeAsStringIn
     * @param currentJulianDate
     * @param app 
     */
    public JTrackingPanel(Hashtable<String,AbstractSatellite> satHash, Hashtable<String,GroundStation> gsHash, String timeAsStringIn, Time currentJulianDate, JSatTrak app)
    {
        this.satHash = satHash;
        this.gsHash = gsHash;
        
        this.currentJulianDate = currentJulianDate;
        
        this.app = app;
                
        initComponents();
        
        // fill out choice boxes
        refreshComboBoxes();
        
        // do this after components INI
        this.timeAsString = timeAsStringIn;
        updateTime(timeAsString);
        
        
        // update pass table 
        passTableModel = new DefaultTableModel();
        passTable.setModel(passTableModel);
        
        passTableModel.addColumn("#"); // pass number
        passTableModel.addColumn("Rise Time"); // 
        passTableModel.addColumn("Set Time"); // 
        passTableModel.addColumn("Duration [Sec]"); // 
        passTableModel.addColumn("Visibility"); //
        
        //passTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        passTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        //passTable.setAutoCreateRowSorter(true);
        
        // add row highliters to passTable --oops not a swingx table
        //passTable.addHighlighter(new ColorHighlighter(HighlightPredicate.EVEN, Color.WHITE, Color.black)); // even, background, foregrond
        //passTable.addHighlighter(new ColorHighlighter(HighlightPredicate.ODD, new Color(229, 229, 229), Color.black)); // odd, background, foregrond
        
        
    }
    
    public void refreshComboBoxes()
    {
        gsComboBox.removeAllItems();
        for(GroundStation gs : gsHash.values())
        {
            gsComboBox.addItem(gs.getStationName());
        }
        
        satComboBox.removeAllItems();
        for(AbstractSatellite sat : satHash.values())
        {
            satComboBox.addItem(sat.getName());
        }
        
                
    } // refreshComboBoxes
    
    // update time called .. update info in box
    // in future might not want to search hash each iteration for gs and sat objects
    public void updateTime(String timeAsString)
    {
        this.timeAsString = timeAsString;
        
        if( gsComboBox.getSelectedIndex() < 0 ||  satComboBox.getSelectedIndex() < 0)
        {
            // something not set
            // erase all data
            aerTextArea.setText("");
            aerTextArea.setBackground(Color.white);
            
            return;
        }
        
        // get sat and gs objects they are null (otherwise same or if boxes changed?)
        // for now local vars = get them each time.
        GroundStation gs = gsHash.get(gsComboBox.getSelectedItem().toString());
        AbstractSatellite sat = satHash.get(satComboBox.getSelectedItem().toString());
        
        // calculate AER
        //double[] aer = gs.calculate_AER( sat.getJ2000Position() ); // in J2k position - incorrect
        if(sat.getPosMOD() != null && !nanDbl.equals(sat.getPosMOD()[0])) // check for NAN
        {
            double[] aer = gs.calculate_AER(sat.getPosMOD());  // MOD

            // add text AER and string
            aerTextArea.setText(timeAsString + "\n\n" + "Azimuth [deg]: " + aer[0] + "\nElevation [deg]: " + aer[1] + "\nRange [m]: " + aer[2]);

            // if in view set BG color to green else white
            if (aer[1] >= gs.getElevationConst())
            {
                aerTextArea.setBackground(Color.GREEN);
            }
            else
            {
                aerTextArea.setBackground(Color.WHITE);
            }

            // update ploar plot
            jPolarPlotLabel.setCurrentAE(aer);
            jPolarPlotLabel.setElevConst(gs.getElevationConst());
            jPolarPlotLabel.setTimeString(timeAsString);
            jPolarPlotLabel.setGs2SatNameString(gs.getStationName().trim() + " to " + sat.getName().trim());

            // check to see if we need to update Lead/Lag data
// no good J2000 positions not saved through time right now in satprops  
            // see if we even need to bother - lead data option selected in both 2D and polar plot
            if (jPolarPlotLabel.isShowLeadLagData() && sat.getShowGroundTrack())
            {
                boolean updateLeadData = false;
                boolean updateLagData = false;

                // need to update lead data?
                if (jPolarPlotLabel.getAerLead() == null || jPolarPlotLabel.getAerLead().length < 1)
                {
                    updateLeadData = true;
                    oldLeadX = sat.getModPosLead()[0][0];
                }
                else
                {
                    if (oldLeadX != sat.getModPosLead()[0][0])
                    {
                        updateLeadData = true;
                        oldLeadX = sat.getModPosLead()[0][0];
                    }
                }

                // need to update lag data?
                if (jPolarPlotLabel.getAerLag() == null || jPolarPlotLabel.getAerLag().length < 1)
                {
                    updateLagData = true;
                    oldLagX = sat.getModPosLag()[0][0];
                }
                else
                {
                    if (oldLagX != sat.getModPosLag()[0][0])
                    {
                        updateLagData = true;
                        oldLagX = sat.getModPosLag()[0][0];
                    }
                }

                // update Lead data if needed
                if (updateLeadData) //|| check to see if lead/lag data has been updated..
                {
                    jPolarPlotLabel.setAerLead(AER.calculate_AER(new double[]{gs.getLatitude(), gs.getLongitude(), gs.getAltitude()}, sat.getModPosLead(), sat.getTimeLead()));
                //System.out.println("Lead updated");
                }

                // update lag data if needed
                if (updateLagData) //|| heck to see if lead/lag data has been updated..
                {
                    jPolarPlotLabel.setAerLag(AER.calculate_AER(new double[]{gs.getLatitude(), gs.getLongitude(), gs.getAltitude()}, sat.getModPosLag(), sat.getTimeLag()));
                //System.out.println("Lag updated");
                }

            } // lead / lag data shown
            
        } // NAN check
        else
        {
            aerTextArea.setText(timeAsString + "\n\n" + "Satellite Ephemeris Not Available");
            aerTextArea.setBackground(Color.RED);
            
            jPolarPlotLabel.setTimeString(timeAsString);
            jPolarPlotLabel.setGs2SatNameString(gs.getStationName().trim() + " to " + sat.getName().trim());
            
            // clear lead/lag data
            jPolarPlotLabel.clearLeadLagData();
            // clear current point?
            jPolarPlotLabel.resetCurrentPosition();
        }
        
        jPolarPlotLabel.repaint(); // repaint
        
    } //updateTime
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        gsComboBox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        satComboBox = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        aerTextArea = new javax.swing.JTextArea();
        refreshComboBoxesButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPolarPlotLabel = new jsattrak.gui.JPolarPlotLabel();
        leadLagCheckBox = new javax.swing.JCheckBox();
        jCheckBox1 = new javax.swing.JCheckBox();
        jCheckBox2 = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        passTable = new javax.swing.JTable();
        jLabel4 = new javax.swing.JLabel();
        timeSpanTextField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        timeStepTextField = new javax.swing.JTextField();
        runPassPredictionButton = new javax.swing.JButton();
        visibleOnlyCheckBox = new javax.swing.JCheckBox();
        go2passButton = new javax.swing.JButton();

        jLabel1.setText("Ground Station:"); // NOI18N

        gsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gsComboBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                gsComboBoxActionPerformed(evt);
            }
        });

        jLabel2.setText("Satellite:"); // NOI18N

        satComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        satComboBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                satComboBoxActionPerformed(evt);
            }
        });

        jLabel3.setText("Current Tracking Information:"); // NOI18N

        aerTextArea.setColumns(20);
        aerTextArea.setRows(5);
        jScrollPane1.setViewportView(aerTextArea);

        refreshComboBoxesButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/gnome_2_18/view-refresh.png"))); // NOI18N
        refreshComboBoxesButton.setToolTipText("refresh sat and ground stations"); // NOI18N
        refreshComboBoxesButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                refreshComboBoxesButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(satComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(gsComboBox, 0, 169, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(refreshComboBoxesButton))
                    .addComponent(jLabel3))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(gsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(satComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(refreshComboBoxesButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 142, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Basic", jPanel1);

        jPolarPlotLabel.setBackground(new java.awt.Color(0, 0, 0));

        leadLagCheckBox.setSelected(true);
        leadLagCheckBox.setText("Show Lead/Lag Data"); // NOI18N
        leadLagCheckBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                leadLagCheckBoxActionPerformed(evt);
            }
        });

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Display Time"); // NOI18N
        jCheckBox1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jCheckBox1ActionPerformed(evt);
            }
        });

        jCheckBox2.setText("Display Names"); // NOI18N
        jCheckBox2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jCheckBox2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPolarPlotLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(leadLagCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCheckBox1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox2)
                .addGap(53, 53, 53))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPolarPlotLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(leadLagCheckBox)
                    .addComponent(jCheckBox1)
                    .addComponent(jCheckBox2)))
        );

        jTabbedPane1.addTab("Polar Plot", jPanel2);

        jLabel6.setFont(new java.awt.Font("Tahoma", 1, 12));
        jLabel6.setText("Pass Predictions:"); // NOI18N

        passTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String []
            {
                "#", "Rise Time", "Set Time"
            }
        )
        {
            Class[] types = new Class []
            {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean []
            {
                false, true, true
            };

            public Class getColumnClass(int columnIndex)
            {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(passTable);
        passTable.getColumnModel().getColumn(0).setHeaderValue("#");
        passTable.getColumnModel().getColumn(1).setHeaderValue("Rise Time");
        passTable.getColumnModel().getColumn(2).setHeaderValue("Set Time");

        jLabel4.setText("Time Span [Days]:"); // NOI18N

        timeSpanTextField.setText("10"); // NOI18N

        jLabel5.setText("Time Step [sec]:"); // NOI18N

        timeStepTextField.setText("60.0"); // NOI18N

        runPassPredictionButton.setText("Calculate"); // NOI18N
        runPassPredictionButton.setToolTipText("Calculate Pass Predictions");
        runPassPredictionButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                runPassPredictionButtonActionPerformed(evt);
            }
        });

        visibleOnlyCheckBox.setText("Visible Only"); // NOI18N

        go2passButton.setText("Go to Pass");
        go2passButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                go2passButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)
                        .addContainerGap())
                    .addComponent(jLabel6)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(timeSpanTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(timeStepTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(54, 54, 54))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(visibleOnlyCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(go2passButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 109, Short.MAX_VALUE)
                        .addComponent(runPassPredictionButton)
                        .addContainerGap())))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(timeSpanTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(timeStepTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(visibleOnlyCheckBox)
                    .addComponent(go2passButton)
                    .addComponent(runPassPredictionButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 142, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Pass Predictions", jPanel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void refreshComboBoxesButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_refreshComboBoxesButtonActionPerformed
    {//GEN-HEADEREND:event_refreshComboBoxesButtonActionPerformed
        // save selections (if there are any)
        String oldSat = "null";
        if(satComboBox.getSelectedIndex() >= 0 )
        {
            oldSat = satComboBox.getSelectedItem().toString();
        }
        String oldGS = "null";
        if(gsComboBox.getSelectedIndex() >= 0 )
        {
            oldGS = gsComboBox.getSelectedItem().toString();
        }
        
        refreshComboBoxes();
        
        // select old selections if there are any
        if(!oldSat.equalsIgnoreCase("null"))
        {
            satComboBox.setSelectedItem(oldSat);
        }
        
        if(!oldGS.equalsIgnoreCase("null"))
        {
            gsComboBox.setSelectedItem(oldGS);
        }
        
    }//GEN-LAST:event_refreshComboBoxesButtonActionPerformed

    private void gsComboBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_gsComboBoxActionPerformed
    {//GEN-HEADEREND:event_gsComboBoxActionPerformed
        aerTextArea.setText(""); // clear box
        if(timeAsString != null)
            updateTime(timeAsString);
        
        objectChangeReset();
        
    }//GEN-LAST:event_gsComboBoxActionPerformed

    private void satComboBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_satComboBoxActionPerformed
    {//GEN-HEADEREND:event_satComboBoxActionPerformed
        aerTextArea.setText(""); // clear box
        if(timeAsString != null)
            updateTime(timeAsString);
        
        objectChangeReset();
    }//GEN-LAST:event_satComboBoxActionPerformed

    private void objectChangeReset()
    {
        // GUI changes needed when objects are changed
        
        // clear pass table
        if (passTableModel != null) // ini of object
        {
            while (passTableModel.getRowCount() > 0)
            {
                passTableModel.removeRow(0);
            }

            // refresh lead/lag in polar plot if needed
            if (gsComboBox.getSelectedIndex() < 0 || satComboBox.getSelectedIndex() < 0)
            {
                return; // something not selected skip lead/lag update
            }

            GroundStation gs = gsHash.get(gsComboBox.getSelectedItem().toString());
            AbstractSatellite sat = satHash.get(satComboBox.getSelectedItem().toString());
            if (jPolarPlotLabel.isShowLeadLagData() && sat.getShowGroundTrack())
            {
                jPolarPlotLabel.setAerLead(AER.calculate_AER(new double[]{gs.getLatitude(), gs.getLongitude(), gs.getAltitude()}, sat.getModPosLead(), sat.getTimeLead()));
                jPolarPlotLabel.setAerLag(AER.calculate_AER(new double[]{gs.getLatitude(), gs.getLongitude(), gs.getAltitude()}, sat.getModPosLag(), sat.getTimeLag()));
            }
        }
        
    }
    
    private void leadLagCheckBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_leadLagCheckBoxActionPerformed
    {//GEN-HEADEREND:event_leadLagCheckBoxActionPerformed
        jPolarPlotLabel.setShowLeadLagData(leadLagCheckBox.isSelected());
        jPolarPlotLabel.repaint();
    }//GEN-LAST:event_leadLagCheckBoxActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jCheckBox1ActionPerformed
    {//GEN-HEADEREND:event_jCheckBox1ActionPerformed
        jPolarPlotLabel.setDisplayTime(jCheckBox1.isSelected());
        jPolarPlotLabel.repaint();
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jCheckBox2ActionPerformed
    {//GEN-HEADEREND:event_jCheckBox2ActionPerformed
        jPolarPlotLabel.setDisplayNames(jCheckBox2.isSelected());
        jPolarPlotLabel.repaint();
    }//GEN-LAST:event_jCheckBox2ActionPerformed

    private void runPassPredictionButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_runPassPredictionButtonActionPerformed
    {//GEN-HEADEREND:event_runPassPredictionButtonActionPerformed
        // get info:
        double timeSpanDays = Double.parseDouble( timeSpanTextField.getText() );
        double timeStepSec = Double.parseDouble( timeStepTextField.getText() );
        boolean onlyVisible = visibleOnlyCheckBox.isSelected();
        
        // get sat and GS
        GroundStation gs = gsHash.get(gsComboBox.getSelectedItem().toString());
        AbstractSatellite sat = satHash.get(satComboBox.getSelectedItem().toString());
        
        // start time Jul Date
        double jdStart = currentJulianDate.getJulianDate();
        
        // clear hash
        passHash.clear();
        
        // clear the table
        while(passTableModel.getRowCount() > 0)
        {
            passTableModel.removeRow(0);
        }
        
        // Sun sun
        
//        // elevation data used in seach
//        double h0=0,h1=0,h2=0;
//        
//        // intially calculate elevations at 0/1 time points
//        // AER.calculate_AER(new double[]{gs.getLatitude(), gs.getLongitude(), gs.getAltitude()}, sat.getModPosLead(), sat.getTimeLead())
//        double time0 = jdStart - timeStepSec/(60.0*60.0*24.0);
//        h0 = AER.calculate_AER(gs.getLla_deg_m(), sat.calculateMODPositionFromUT(time0) , time0)[1];
//        double time1 = jdStart;
//        h1 = AER.calculate_AER(gs.getLla_deg_m(), sat.calculateMODPositionFromUT(time1) , time1)[1];
//        double time2 = jdStart + timeStepSec/(60.0*60.0*24.0);; // declare var
//        
//        System.out.println(time0 + "," + h0 );
//        System.out.println(time1 + "," + h1 );
//        
//        // use quadratic fit to search for zeros
//        for(double jd = jdStart; jd <= jdStart + timeSpanDays; jd += timeStepSec/(60.0*60.0*24.0))
//        {
//            time0 = time1;
//            time1 = time2;
//            time2 = jd + timeStepSec/(60.0*60.0*24.0);
//            
//            // calculate elevations at each time step (if needed)
//            h0 = h1;
//            h1 = h2;
//            // calculate the elevation at this newly visited point
//            h2 = AER.calculate_AER(gs.getLla_deg_m(), sat.calculateMODPositionFromUT(time2) , time2)[1];
//            System.out.println(time2 + "," + h2 );
//            
//            // create a quadratic interpolator
//            QuadraticInterpolatorSimp qis = new QuadraticInterpolatorSimp(time0, h0, time1, h1, time2, h2);
//            
////            if(qis.getRootCountInDomain() == 1)
////            {
////                System.out.println("Event(1): " + qis.getLowerRoot() );
////            }else if(qis.getRootCountInDomain() == 2)
////            {
////                System.out.println("Event(2a): " + qis.getLowerRoot() );
////                System.out.println("Event(2b): " + qis.getUpperRoot() );
////            }
//            
//        } // for loop seaching for rise/set events
        
        
        Sun internalSun = new Sun(jdStart - AstroConst.JDminusMJD); // new sun for internal calculations of Visibility
        
        // linear search
        double time0, h0;
        double time1 = jdStart;
        double h1 = AER.calculate_AER(gs.getLla_deg_m(), sat.calculateMODPositionFromUT(time1) , time1)[1] - gs.getElevationConst();
        
        int passCount = 0;
        
        if(h1 > 0)
        {
            passCount++;
            passTableModel.addRow(new Object[] {passCount,"--","","",""});
        }
        
        double lastRise = 0;
        
        for(double jd = jdStart; jd <= jdStart + timeSpanDays; jd += timeStepSec/(60.0*60.0*24.0))
        {
            time0 = time1;
            time1 = jd + timeStepSec/(60.0*60.0*24.0);
            
            // calculate elevations at each time step (if needed)
            h0 = h1;
            // calculate the elevation at this newly visited point
            h1 = AER.calculate_AER(gs.getLla_deg_m(), sat.calculateMODPositionFromUT(time1) , time1)[1] - gs.getElevationConst();
            
            // rise
            if(h0<=0 && h1 >0)
            {
                double riseTime =  findSatRiseSetRoot(sat, gs, time0, time1, h0, h1);
                //System.out.println("Rise at " + riseTime  + " (" + time0 + "," + time1 + ")"); 
                
                lastRise = riseTime; // save
                
                // add to table
                passCount++;
                // use Time object to convert Julian date to string using program settings (i.e. time zone)
                String crossTimeStr = currentJulianDate.convertJD2String(riseTime);
                
                passTableModel.addRow(new Object[] {passCount,crossTimeStr,"","",""});
            }
            
            // set
            if(h1<=0 && h0 >0)
            {
                double setTime =  findSatRiseSetRoot(sat, gs, time0, time1, h0, h1);
                //System.out.println("Set at " + setTime  + " (" + time0 + "," + time1 + ")"); 
                
                // add to table
                String crossTimeStr = currentJulianDate.convertJD2String(setTime);
                passTableModel.setValueAt(crossTimeStr, passTableModel.getRowCount()-1, 2); // last row, 3rd column (2)
                
                // add duration
                if(lastRise > 0)
                {
                    DecimalFormat fmt2Dig = new DecimalFormat("00.000");
                    
                    double duration = (setTime - lastRise)*24.0*60.0*60.0; // seconds
                    String durStr = fmt2Dig.format( duration );
                    passTableModel.setValueAt(durStr, passTableModel.getRowCount()-1, 3); // last row, 4rd column (3)
                }
                
                // determine visibility
                if(lastRise > 0)
                {
                    // Visiable, Radar Night, Radar Night
                    
                    // use the time 1/2 between rise and set for viz calculations
                    // DOES NOT CHECK FOR VIS NEAR END POINTS SO COULD MISS SOME PARTIAL PASS VISIBILITY
                                        
                    double julDateVizCalc = (setTime - lastRise)/2.0 + lastRise;
                    
                    // SAVE to hash - for use later
                    passHash.put(new Integer(passCount), new Double(julDateVizCalc));
                    
                    // twilight offset
                    // 7 seems good
                    // 6 is used by heavens-above.com
                    double twilightOffset = 6; // degrees extra required for darkness
                    
                    // set the suns time 
                    internalSun.setCurrentMJD( julDateVizCalc - AstroConst.JDminusMJD );
                    
                    // MOD - sun dot site positions to determine if station is in sunlight
                    double[] gsECI = AER.calculateECIposition(julDateVizCalc, gs.getLla_deg_m());
                    double sunDotSite = MathUtils.dot(internalSun.getCurrentPositionMOD(),gsECI );
                    
                    // TEST - find angle between sun -> center of Earth -> Ground Station
                    double sinFinalSigmaGS = MathUtils.norm( MathUtils.cross(internalSun.getCurrentPositionMOD(), gsECI) ) 
                             / ( MathUtils.norm(internalSun.getCurrentPositionMOD()) * MathUtils.norm(gsECI)  ); 
                    double finalSigmaGS = Math.asin(sinFinalSigmaGS)*180.0/Math.PI; // in degrees
                    
                    if(sunDotSite > 0 || (90.0-finalSigmaGS) < twilightOffset )
                    {
                        passTableModel.setValueAt("Radar Sun", passTableModel.getRowCount()-1, 4); // last row, 5rd column (4)
                    } // sun light
                    else
                    {
                        // now we know the site is in darkness - need to figure out if the satelite is in light
                        // use predict algorithm from Vallado 2nd ed.
                        double[] satMOD = sat.calculateMODPositionFromUT(julDateVizCalc);
                        double sinFinalSigma = MathUtils.norm( MathUtils.cross(internalSun.getCurrentPositionMOD(), satMOD) ) 
                                / ( MathUtils.norm(internalSun.getCurrentPositionMOD()) * MathUtils.norm(satMOD)  ); 
                        double finalSigma = Math.asin(sinFinalSigma);
                        double dist = MathUtils.norm(satMOD) * Math.cos( finalSigma - Math.PI/2.0);
                        
                        if(dist > AstroConst.R_Earth)
                        {
                            // sat is in sunlight!
                            passTableModel.setValueAt("Visible", passTableModel.getRowCount()-1, 4); // last row, 5rd column (4)
                        }
                        else  // Radar Night (both in darkness)
                        {
                            passTableModel.setValueAt("Radar Night", passTableModel.getRowCount()-1, 4); // last row, 5rd column (4)
                        }
                        
                    } // site in dark
                    
                } // visibility (with last rise)             
                
            } // set
            
            
        }// linear seach
        
        // if visible only checked remove other items from the list
        if(visibleOnlyCheckBox.isSelected())
        {
            
            int vizColumn = 4;  // vis text column
            for(int i=passTableModel.getRowCount()-1; i>=0;i-- )
            {
               
              if(!passTableModel.getValueAt(i, vizColumn).toString().equalsIgnoreCase("Visible"))
              {
                  passTableModel.removeRow(i);
              }
            }
            
            
            
        } // remove non-visible
        
        
        // set first col to be small
        passTable.getColumnModel().getColumn(0).setPreferredWidth(10);
        
    }//GEN-LAST:event_runPassPredictionButtonActionPerformed

    private void go2passButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_go2passButtonActionPerformed
    {//GEN-HEADEREND:event_go2passButtonActionPerformed
        // get selected row in table
        int tableRow = passTable.getSelectedRow();
        
        if(tableRow >= 0)
        {
            // get # in that row
            Integer passNum = new Integer( passTableModel.getValueAt(tableRow, 0).toString() );
            
            if( passHash.containsKey(passNum) )
            {
                double jdMidPoint = passHash.get(passNum).doubleValue();

                // get milliseconds from Julian Date            
                long millis = currentJulianDate.convertJD2Calendar(jdMidPoint).getTimeInMillis();

                // get current app time
                double daysDiff = Math.abs( currentJulianDate.getJulianDate() - jdMidPoint);
                
                // check to see if lead/lag data needs updating
                app.checkTimeDiffResetGroundTracks(daysDiff);
                        
                // set app to new time
                app.setTime(millis);
            }
        }
                
                
    }//GEN-LAST:event_go2passButtonActionPerformed
    
    // bisection method, crossing time should be bracketed by time0 and time1
    private double findSatRiseSetRoot(AbstractSatellite sat, GroundStation gs, double time0, double time1, double f0, double f1)
    {
        double tol = (1.157407E-5)/4; // 1/4 a sec (in units of a day)
        
        int iterCount = 0;
        
        while(Math.abs(time1-time0) > 2*tol)
        {
            //Calculate midpoint of domain
            double timeMid = (time1 + time0) / 2.0;
            double fmid = AER.calculate_AER(gs.getLla_deg_m(), sat.calculateMODPositionFromUT(timeMid) , timeMid)[1] - gs.getElevationConst();
            
            if( f0 * fmid > 0) // same sign
            {
                // replace f0 with fmid
                f0 = fmid;
                time0 = timeMid;
                
            }else  // else replace f1 with fmid
            {
                f1 = fmid;
                time1 = timeMid;
            }
            
            iterCount++;
        } // while not in tolerance
        
        // return best gues using linear interpolation between last two points
        double a = (f1-f0)/(time1-time0);
        double b = f1-a*time1;
        double riseTime = -b/a;
        
        //System.out.println("Bisection Iters: " + iterCount);
        
        return riseTime; // return best guess  -typically:  (time0 + time1)/2.0;
        //return (time0 + time1)/2.0;
        
    } // findSatRiseSetRoot
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea aerTextArea;
    private javax.swing.JButton go2passButton;
    private javax.swing.JComboBox gsComboBox;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private jsattrak.gui.JPolarPlotLabel jPolarPlotLabel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JCheckBox leadLagCheckBox;
    private javax.swing.JTable passTable;
    private javax.swing.JButton refreshComboBoxesButton;
    private javax.swing.JButton runPassPredictionButton;
    private javax.swing.JComboBox satComboBox;
    private javax.swing.JTextField timeSpanTextField;
    private javax.swing.JTextField timeStepTextField;
    private javax.swing.JCheckBox visibleOnlyCheckBox;
    // End of variables declaration//GEN-END:variables
    
}
