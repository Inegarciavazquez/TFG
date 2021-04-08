package com.example.basesdatos;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import weka.clusterers.NumberOfClustersRequestable;
import weka.clusterers.RandomizableDensityBasedClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.estimators.DiscreteEstimator;
import weka.estimators.Estimator;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

public class EM_modificada extends RandomizableDensityBasedClusterer implements NumberOfClustersRequestable, WeightedInstancesHandler {
    static final long serialVersionUID = 8348181483812829475L;
    private Estimator[][] m_model;
    private double[][][] m_modelNormal;
    private double m_minStdDev = 1.0E-6D;
    private double[] m_minStdDevPerAtt;
    private double[][] m_weights;
    private double[] m_priors;
    private double m_loglikely;
    private Instances m_theInstances = null;
    private int m_num_clusters;
    private int m_initialNumClusters;
    private int m_num_attribs;
    private int m_num_instances;
    private int m_max_iterations;
    private double[] m_minValues;
    private double[] m_maxValues;
    private Random m_rr;
    private boolean m_verbose;
    private ReplaceMissingValues m_replaceMissing;
    private boolean m_displayModelInOldFormat;
    private static double m_normConst = Math.log(Math.sqrt(6.283185307179586D));

    public String globalInfo() {
        return "Simple EM (expectation maximisation) class.\n\nEM assigns a probability distribution to each instance which indicates the probability of it belonging to each of the clusters. EM can decide how many clusters to create by cross validation, or you may specify apriori how many clusters to generate.\n\nThe cross validation performed to determine the number of clusters is done in the following steps:\n1. the number of clusters is set to 1\n2. the training set is split randomly into 10 folds.\n3. EM is performed 10 times using the 10 folds the usual CV way.\n4. the loglikelihood is averaged over all 10 results.\n5. if loglikelihood has increased the number of clusters is increased by 1 and the program continues at step 2. \n\nThe number of folds is fixed to 10, as long as the number of instances in the training set is not smaller 10. If this is the case the number of folds is set equal to the number of instances.";
    }

    public Enumeration listOptions() {
        Vector result = new Vector();
        result.addElement(new Option("\tnumber of clusters. If omitted or -1 specified, then \n\tcross validation is used to select the number of clusters.", "N", 1, "-N <num>"));
        result.addElement(new Option("\tmax iterations.\n(default 100)", "I", 1, "-I <num>"));
        result.addElement(new Option("\tverbose.", "V", 0, "-V"));
        result.addElement(new Option("\tminimum allowable standard deviation for normal density\n\tcomputation\n\t(default 1e-6)", "M", 1, "-M <num>"));
        result.addElement(new Option("\tDisplay model in old format (good when there are many clusters)\n", "O", 0, "-O"));
        Enumeration en = super.listOptions();

        while(en.hasMoreElements()) {
            result.addElement(en.nextElement());
        }

        return result.elements();
    }

    public void setOptions(String[] options) throws Exception {
        this.resetOptions();
        this.setDebug(Utils.getFlag('V', options));
        String optionString = Utils.getOption('I', options);
        if (optionString.length() != 0) {
            this.setMaxIterations(Integer.parseInt(optionString));
        }

        optionString = Utils.getOption('N', options);
        if (optionString.length() != 0) {
            this.setNumClusters(Integer.parseInt(optionString));
        }

        optionString = Utils.getOption('M', options);
        if (optionString.length() != 0) {
            this.setMinStdDev(new Double(optionString));
        }

        this.setDisplayModelInOldFormat(Utils.getFlag('O', options));
        super.setOptions(options);
    }

    public String displayModelInOldFormatTipText() {
        return "Use old format for model output. The old format is better when there are many clusters. The new format is better when there are fewer clusters and many attributes.";
    }

    public void setDisplayModelInOldFormat(boolean d) {
        this.m_displayModelInOldFormat = d;
    }

    public boolean getDisplayModelInOldFormat() {
        return this.m_displayModelInOldFormat;
    }

    public String minStdDevTipText() {
        return "set minimum allowable standard deviation";
    }

    public void setMinStdDev(double m) {
        this.m_minStdDev = m;
    }

    public void setMinStdDevPerAtt(double[] m) {
        this.m_minStdDevPerAtt = m;
    }

    public double getMinStdDev() {
        return this.m_minStdDev;
    }

    public String numClustersTipText() {
        return "set number of clusters. -1 to select number of clusters automatically by cross validation.";
    }

    public void setNumClusters(int n) throws Exception {
        if (n == 0) {
            throw new Exception("Number of clusters must be > 0. (or -1 to select by cross validation).");
        } else {
            if (n < 0) {
                this.m_num_clusters = -1;
                this.m_initialNumClusters = -1;
            } else {
                this.m_num_clusters = n;
                this.m_initialNumClusters = n;
            }

        }
    }

    public int getNumClusters() {
        return this.m_initialNumClusters;
    }

    public String maxIterationsTipText() {
        return "maximum number of iterations";
    }

    public void setMaxIterations(int i) throws Exception {
        if (i < 1) {
            throw new Exception("Maximum number of iterations must be > 0!");
        } else {
            this.m_max_iterations = i;
        }
    }

    public int getMaxIterations() {
        return this.m_max_iterations;
    }

    public String debugTipText() {
        return "If set to true, clusterer may output additional info to the console.";
    }

    public void setDebug(boolean v) {
        this.m_verbose = v;
    }

    public boolean getDebug() {
        return this.m_verbose;
    }

    public String[] getOptions() {
        Vector result = new Vector();
        result.add("-I");
        result.add("" + this.m_max_iterations);
        result.add("-N");
        result.add("" + this.getNumClusters());
        result.add("-M");
        result.add("" + this.getMinStdDev());
        if (this.m_displayModelInOldFormat) {
            result.add("-O");
        }

        String[] options = super.getOptions();

        for(int i = 0; i < options.length; ++i) {
            result.add(options[i]);
        }

        return (String[])result.toArray(new String[result.size()]);
    }

    private void EM_Init(Instances inst) throws Exception {
        SimpleKMeans bestK = null;
        double bestSqE = 1.7976931348623157E308D;

        int i;
        for(i = 0; i < 50; ++i) {
            SimpleKMeans sk = new SimpleKMeans();
            sk.setSeed(this.m_rr.nextInt());
            sk.setNumClusters(this.m_num_clusters);
            sk.setDisplayStdDevs(true);
            sk.buildClusterer(inst);
            if (sk.getSquaredError() < bestSqE) {
                bestSqE = sk.getSquaredError();
                bestK = sk;
            }
        }

        this.m_num_clusters = bestK.numberOfClusters();
        this.m_weights = new double[inst.numInstances()][this.m_num_clusters];
        this.m_model = new DiscreteEstimator[this.m_num_clusters][this.m_num_attribs];
        this.m_modelNormal = new double[this.m_num_clusters][this.m_num_attribs][3];
        this.m_priors = new double[this.m_num_clusters];
        Instances centers = bestK.getClusterCentroids();
        Instances stdD = bestK.getClusterStandardDevs();
        int[][][] nominalCounts = bestK.getClusterNominalCounts();
        int[] clusterSizes = bestK.getClusterSizes();

        int j;
        for(i = 0; i < this.m_num_clusters; ++i) {
            Instance center = centers.instance(i);

            for(j = 0; j < this.m_num_attribs; ++j) {
                if (inst.attribute(j).isNominal()) {
                    this.m_model[i][j] = new DiscreteEstimator(this.m_theInstances.attribute(j).numValues(), true);

                    for(int k = 0; k < inst.attribute(j).numValues(); ++k) {
                        this.m_model[i][j].addValue((double)k, (double)nominalCounts[i][j][k]);
                    }
                } else {
                    double minStdD = this.m_minStdDevPerAtt != null ? this.m_minStdDevPerAtt[j] : this.m_minStdDev;
                    double mean = center.isMissing(j) ? inst.meanOrMode(j) : center.value(j);
                    this.m_modelNormal[i][j][0] = mean;
                    double stdv = stdD.instance(i).isMissing(j) ? (this.m_maxValues[j] - this.m_minValues[j]) / (double)(2 * this.m_num_clusters) : stdD.instance(i).value(j);
                    if (stdv < minStdD) {
                        stdv = inst.attributeStats(j).numericStats.stdDev;
                        if (Double.isInfinite(stdv)) {
                            stdv = minStdD;
                        }

                        if (stdv < minStdD) {
                            stdv = minStdD;
                        }
                    }

                    if (stdv <= 0.0D) {
                        stdv = this.m_minStdDev;
                    }

                    this.m_modelNormal[i][j][1] = stdv;
                    this.m_modelNormal[i][j][2] = 1.0D;
                }
            }
        }

        for(j = 0; j < this.m_num_clusters; ++j) {
            this.m_priors[j] = (double)clusterSizes[j];
        }

        Utils.normalize(this.m_priors);
    }

    private void estimate_priors(Instances inst) throws Exception {
        int i;
        for(i = 0; i < this.m_num_clusters; ++i) {
            this.m_priors[i] = 0.0D;
        }

        for(i = 0; i < inst.numInstances(); ++i) {
            for(int j = 0; j < this.m_num_clusters; ++j) {
                double[] var10000 = this.m_priors;
                var10000[j] += inst.instance(i).weight() * this.m_weights[i][j];
            }
        }

        Utils.normalize(this.m_priors);
    }

    private double logNormalDens(double x, double mean, double stdDev) {
        double diff = x - mean;
        return -(diff * diff / (2.0D * stdDev * stdDev)) - m_normConst - Math.log(stdDev);
    }

    private void new_estimators() {
        for(int i = 0; i < this.m_num_clusters; ++i) {
            for(int j = 0; j < this.m_num_attribs; ++j) {
                if (this.m_theInstances.attribute(j).isNominal()) {
                    this.m_model[i][j] = new DiscreteEstimator(this.m_theInstances.attribute(j).numValues(), true);
                } else {
                    this.m_modelNormal[i][j][0] = this.m_modelNormal[i][j][1] = this.m_modelNormal[i][j][2] = 0.0D;
                }
            }
        }

    }

    private void M(Instances inst) throws Exception {
        this.new_estimators();
        this.estimate_priors(inst);

        double[] var10000;
        int i;
        int j;
        for(i = 0; i < this.m_num_clusters; ++i) {
            for(j = 0; j < this.m_num_attribs; ++j) {
                for(int l = 0; l < inst.numInstances(); ++l) {
                    Instance in = inst.instance(l);
                    if (!in.isMissing(j)) {
                        if (inst.attribute(j).isNominal()) {
                            this.m_model[i][j].addValue(in.value(j), in.weight() * this.m_weights[l][i]);
                        } else {
                            var10000 = this.m_modelNormal[i][j];
                            var10000[0] += in.value(j) * in.weight() * this.m_weights[l][i];
                            var10000 = this.m_modelNormal[i][j];
                            var10000[2] += in.weight() * this.m_weights[l][i];
                            var10000 = this.m_modelNormal[i][j];
                            var10000[1] += in.value(j) * in.value(j) * in.weight() * this.m_weights[l][i];
                        }
                    }
                }
            }
        }

        for(j = 0; j < this.m_num_attribs; ++j) {
            if (!inst.attribute(j).isNominal()) {
                for(i = 0; i < this.m_num_clusters; ++i) {
                    if (this.m_modelNormal[i][j][2] <= 0.0D) {
                        this.m_modelNormal[i][j][1] = 1.7976931348623157E308D;
                        this.m_modelNormal[i][j][0] = this.m_minStdDev;
                    } else {
                        this.m_modelNormal[i][j][1] = (this.m_modelNormal[i][j][1] - this.m_modelNormal[i][j][0] * this.m_modelNormal[i][j][0] / this.m_modelNormal[i][j][2]) / this.m_modelNormal[i][j][2];
                        if (this.m_modelNormal[i][j][1] < 0.0D) {
                            this.m_modelNormal[i][j][1] = 0.0D;
                        }

                        double minStdD = this.m_minStdDevPerAtt != null ? this.m_minStdDevPerAtt[j] : this.m_minStdDev;
                        this.m_modelNormal[i][j][1] = Math.sqrt(this.m_modelNormal[i][j][1]);
                        if (this.m_modelNormal[i][j][1] <= minStdD) {
                            this.m_modelNormal[i][j][1] = inst.attributeStats(j).numericStats.stdDev;
                            if (this.m_modelNormal[i][j][1] <= minStdD) {
                                this.m_modelNormal[i][j][1] = minStdD;
                            }
                        }

                        if (this.m_modelNormal[i][j][1] <= 0.0D) {
                            this.m_modelNormal[i][j][1] = this.m_minStdDev;
                        }

                        if (Double.isInfinite(this.m_modelNormal[i][j][1])) {
                            this.m_modelNormal[i][j][1] = this.m_minStdDev;
                        }

                        var10000 = this.m_modelNormal[i][j];
                        var10000[0] /= this.m_modelNormal[i][j][2];
                    }
                }
            }
        }

    }

    private double E(Instances inst, boolean change_weights) throws Exception {
        double loglk = 0.0D;
        double sOW = 0.0D;

        for(int l = 0; l < inst.numInstances(); ++l) {
            Instance in = inst.instance(l);
            loglk += in.weight() * this.logDensityForInstance(in);
            sOW += in.weight();
            if (change_weights) {
                this.m_weights[l] = this.distributionForInstance(in);
            }
        }

        return loglk / sOW;
    }

    public EM_modificada() {
        this.m_SeedDefault = 100;
        this.resetOptions();
    }

    protected void resetOptions() {
        this.m_minStdDev = 1.0E-6D;
        this.m_max_iterations = 100;
        this.m_Seed = this.m_SeedDefault;
        this.m_num_clusters = -1;
        this.m_initialNumClusters = -1;
        this.m_verbose = false;
    }

    public double[][][] getClusterModelsNumericAtts() {
        return this.m_modelNormal;
    }

    public double[] getClusterPriors() {
        return this.m_priors;
    }

    public String toString() {
        if (this.m_displayModelInOldFormat) {
            return this.toStringOriginal();
        } else if (this.m_priors == null) {
            return "No clusterer built yet!";
        } else {
            StringBuffer temp = new StringBuffer();
            temp.append("\nEM\n==\n");
            if (this.m_initialNumClusters == -1) {
                temp.append("\nNumber of clusters selected by cross validation: " + this.m_num_clusters + "\n");
            } else {
                temp.append("\nNumber of clusters: " + this.m_num_clusters + "\n");
            }

            int maxWidth = 0;
            int maxAttWidth = 0;
            boolean containsKernel = false;

            int i;
            String total;
            for(i = 0; i < this.m_num_attribs; ++i) {
                Attribute a = this.m_theInstances.attribute(i);
                if (a.name().length() > maxAttWidth) {
                    maxAttWidth = this.m_theInstances.attribute(i).name().length();
                }

                if (a.isNominal()) {
                    for(int j = 0; j < a.numValues(); ++j) {
                        total = a.value(j) + "  ";
                        if (total.length() > maxAttWidth) {
                            maxAttWidth = total.length();
                        }
                    }
                }
            }

            String val;
            int j;
            for(i = 0; i < this.m_num_clusters; ++i) {
                for( j = 0; j < this.m_num_attribs; ++j) {
                    if (this.m_theInstances.attribute(j).isNumeric()) {
                        double mean = Math.log(Math.abs(this.m_modelNormal[i][j][0])) / Math.log(10.0D);
                        double stdD = Math.log(Math.abs(this.m_modelNormal[i][j][1])) / Math.log(10.0D);
                        double width = mean > stdD ? mean : stdD;
                        if (width < 0.0D) {
                            width = 1.0D;
                        }

                        width += 6.0D;
                        if ((int)width > maxWidth) {
                            maxWidth = (int)width;
                        }
                    } else {
                        DiscreteEstimator d = (DiscreteEstimator)this.m_model[i][j];

                        for(j = 0; j < d.getNumSymbols(); ++j) {
                            val = Utils.doubleToString(d.getCount((double)j), maxWidth, 4).trim();
                            if (val.length() > maxWidth) {
                                maxWidth = val.length();
                            }
                        }

                        j = Utils.doubleToString(d.getSumOfCounts(), maxWidth, 4).trim().length();
                        if (j > maxWidth) {
                            maxWidth = j;
                        }
                    }
                }
            }

            if (maxAttWidth < "Attribute".length()) {
                maxAttWidth = "Attribute".length();
            }

            maxAttWidth += 2;
            temp.append("\n\n");
            temp.append(this.pad("Cluster", " ", maxAttWidth + maxWidth + 1 - "Cluster".length(), true));
            temp.append("\n");
            temp.append(this.pad("Attribute", " ", maxAttWidth - "Attribute".length(), false));

            String attName;
            for(i = 0; i < this.m_num_clusters; ++i) {
                attName = "" + i;
                temp.append(this.pad(attName, " ", maxWidth + 1 - attName.length(), true));
            }

            temp.append("\n");
            temp.append(this.pad("", " ", maxAttWidth, true));

            for(i = 0; i < this.m_num_clusters; ++i) {
                attName = Utils.doubleToString(this.m_priors[i], maxWidth, 2).trim();
                attName = "(" + attName + ")";
                temp.append(this.pad(attName, " ", maxWidth + 1 - attName.length(), true));
            }

            temp.append("\n");
            temp.append(this.pad("", "=", maxAttWidth + maxWidth * this.m_num_clusters + this.m_num_clusters + 1, true));
            temp.append("\n");

            for(i = 0; i < this.m_num_attribs; ++i) {
                attName = this.m_theInstances.attribute(i).name();
                temp.append(attName + "\n");
                int k;
                if (this.m_theInstances.attribute(i).isNumeric()) {
                    String meanL = "  mean";
                    temp.append(this.pad(meanL, " ", maxAttWidth + 1 - meanL.length(), false));

                    for(j = 0; j < this.m_num_clusters; ++j) {
                        val = Utils.doubleToString(this.m_modelNormal[j][i][0], maxWidth, 4).trim();
                        temp.append(this.pad(val, " ", maxWidth + 1 - val.length(), true));
                    }

                    temp.append("\n");
                    total = "  std. dev.";
                    temp.append(this.pad(total, " ", maxAttWidth + 1 - total.length(), false));

                    for(k = 0; k < this.m_num_clusters; ++k) {
                        String stdDev = Utils.doubleToString(this.m_modelNormal[k][i][1], maxWidth, 4).trim();
                        temp.append(this.pad(stdDev, " ", maxWidth + 1 - stdDev.length(), true));
                    }

                    temp.append("\n\n");
                } else {
                    Attribute a = this.m_theInstances.attribute(i);

                    for(j = 0; j < a.numValues(); ++j) {
                        val = "  " + a.value(j);
                        temp.append(this.pad(val, " ", maxAttWidth + 1 - val.length(), false));

                        for( k = 0; k < this.m_num_clusters; ++k) {
                            DiscreteEstimator d = (DiscreteEstimator)this.m_model[k][i];
                            String count = Utils.doubleToString(d.getCount((double)j), maxWidth, 4).trim();
                            temp.append(this.pad(count, " ", maxWidth + 1 - count.length(), true));
                        }

                        temp.append("\n");
                    }

                    total = "  [total]";
                    temp.append(this.pad(total, " ", maxAttWidth + 1 - total.length(), false));

                    for(k = 0; k < this.m_num_clusters; ++k) {
                        DiscreteEstimator d = (DiscreteEstimator)this.m_model[k][i];
                        String count = Utils.doubleToString(d.getSumOfCounts(), maxWidth, 4).trim();
                        temp.append(this.pad(count, " ", maxWidth + 1 - count.length(), true));
                    }

                    temp.append("\n");
                }
            }

            return temp.toString();
        }
    }

    private String pad(String source, String padChar, int length, boolean leftPad) {
        StringBuffer temp = new StringBuffer();
        int i;
        if (leftPad) {
            for(i = 0; i < length; ++i) {
                temp.append(padChar);
            }

            temp.append(source);
        } else {
            temp.append(source);

            for(i = 0; i < length; ++i) {
                temp.append(padChar);
            }
        }

        return temp.toString();
    }

    protected String toStringOriginal() {
        if (this.m_priors == null) {
            return "No clusterer built yet!";
        } else {
            StringBuffer temp = new StringBuffer();
            temp.append("\nEM\n==\n");
            if (this.m_initialNumClusters == -1) {
                temp.append("\nNumber of clusters selected by cross validation: " + this.m_num_clusters + "\n");
            } else {
                temp.append("\nNumber of clusters: " + this.m_num_clusters + "\n");
            }

            for(int j = 0; j < this.m_num_clusters; ++j) {
                temp.append("\nCluster: " + j + " Prior probability: " + Utils.doubleToString(this.m_priors[j], 4) + "\n\n");

                for(int i = 0; i < this.m_num_attribs; ++i) {
                    temp.append("Attribute: " + this.m_theInstances.attribute(i).name() + "\n");
                    if (this.m_theInstances.attribute(i).isNominal()) {
                        if (this.m_model[j][i] != null) {
                            temp.append(this.m_model[j][i].toString());
                        }
                    } else {
                        temp.append("Normal Distribution. Mean = " + Utils.doubleToString(this.m_modelNormal[j][i][0], 4) + " StdDev = " + Utils.doubleToString(this.m_modelNormal[j][i][1], 4) + "\n");
                    }
                }
            }

            return temp.toString();
        }
    }

    private void EM_Report(Instances inst) {
        System.out.println("======================================");

        int j;
        for(j = 0; j < this.m_num_clusters; ++j) {
            for(int i = 0; i < this.m_num_attribs; ++i) {
                System.out.println("Clust: " + j + " att: " + i + "\n");
                if (this.m_theInstances.attribute(i).isNominal()) {
                    if (this.m_model[j][i] != null) {
                        System.out.println(this.m_model[j][i].toString());
                    }
                } else {
                    System.out.println("Normal Distribution. Mean = " + Utils.doubleToString(this.m_modelNormal[j][i][0], 8, 4) + " StandardDev = " + Utils.doubleToString(this.m_modelNormal[j][i][1], 8, 4) + " WeightSum = " + Utils.doubleToString(this.m_modelNormal[j][i][2], 8, 4));
                }
            }
        }

        for(int l = 0; l < inst.numInstances(); ++l) {
            int m = Utils.maxIndex(this.m_weights[l]);
            System.out.print("Inst " + Utils.doubleToString((double)l, 5, 0) + " Class " + m + "\t");

            for(j = 0; j < this.m_num_clusters; ++j) {
                System.out.print(Utils.doubleToString(this.m_weights[l][j], 7, 5) + "  ");
            }

            System.out.println();
        }

    }

    private void CVClusters() throws Exception {
        double CVLogLikely = -1.7976931348623157E308D;
        boolean CVincreased = true;
        this.m_num_clusters = 1;
        int num_clusters = this.m_num_clusters;
        int numFolds = this.m_theInstances.numInstances() < 10 ? this.m_theInstances.numInstances() : 10;
        boolean ok = true;
        int seed = this.getSeed();
        int restartCount = 0;

        label73:
        while(CVincreased) {
            CVincreased = false;
            Random cvr = new Random((long)this.getSeed());
            Instances trainCopy = new Instances(this.m_theInstances);
            trainCopy.randomize(cvr);
            double templl = 0.0D;

            for(int i = 0; i < numFolds; ++i) {
                Instances cvTrain = trainCopy.trainCV(numFolds, i, cvr);
                if (num_clusters > cvTrain.numInstances()) {
                    break label73;
                }

                Instances cvTest = trainCopy.testCV(numFolds, i);
                this.m_rr = new Random((long)seed);

                for(int z = 0; z < 10; ++z) {
                    this.m_rr.nextDouble();
                }

                this.m_num_clusters = num_clusters;
                this.EM_Init(cvTrain);

                try {
                    this.iterate(cvTrain, false);
                } catch (Exception var20) {
                    var20.printStackTrace();
                    ++seed;
                    ++restartCount;
                    ok = false;
                    if (restartCount > 5) {
                        break label73;
                    }
                    break;
                }

                double tll;
                try {
                    tll = this.E(cvTest, false);
                } catch (Exception var19) {
                    var19.printStackTrace();
                    ++seed;
                    ++restartCount;
                    ok = false;
                    if (restartCount > 5) {
                        break label73;
                    }
                    break;
                }

                if (this.m_verbose) {
                    System.out.println("# clust: " + num_clusters + " Fold: " + i + " Loglikely: " + tll);
                }

                templl += tll;
            }

            if (ok) {
                restartCount = 0;
                seed = this.getSeed();
                templl /= (double)numFolds;
                if (this.m_verbose) {
                    System.out.println("=================================================\n# clust: " + num_clusters + " Mean Loglikely: " + templl + "\n================================" + "=================");
                }

                if (templl > CVLogLikely) {
                    CVLogLikely = templl;
                    CVincreased = true;
                    ++num_clusters;
                }
            }
        }

        if (this.m_verbose) {
            System.out.println("Number of clusters: " + (num_clusters - 1));
        }

        this.m_num_clusters = num_clusters - 1;
    }

    public int numberOfClusters() throws Exception {
        if (this.m_num_clusters == -1) {
            throw new Exception("Haven't generated any clusters!");
        } else {
            return this.m_num_clusters;
        }
    }

    private void updateMinMax(Instance instance) {
        for(int j = 0; j < this.m_theInstances.numAttributes(); ++j) {
            if (!instance.isMissing(j)) {
                if (Double.isNaN(this.m_minValues[j])) {
                    this.m_minValues[j] = instance.value(j);
                    this.m_maxValues[j] = instance.value(j);
                } else if (instance.value(j) < this.m_minValues[j]) {
                    this.m_minValues[j] = instance.value(j);
                } else if (instance.value(j) > this.m_maxValues[j]) {
                    this.m_maxValues[j] = instance.value(j);
                }
            }
        }

    }

    public Capabilities getCapabilities() {
        Capabilities result = (new SimpleKMeans()).getCapabilities();
        result.setOwner(this);
        return result;
    }

    public void buildClusterer(Instances data) throws Exception {
        this.getCapabilities().testWithFail(data);
        this.m_replaceMissing = new ReplaceMissingValues();
        Instances instances = new Instances(data);
        instances.setClassIndex(-1);
        this.m_replaceMissing.setInputFormat(instances);
        data = Filter.useFilter(instances, this.m_replaceMissing);
        instances = null;
        this.m_theInstances = data;
        this.m_minValues = new double[this.m_theInstances.numAttributes()];
        this.m_maxValues = new double[this.m_theInstances.numAttributes()];

        int i;
        for(i = 0; i < this.m_theInstances.numAttributes(); ++i) {
            this.m_minValues[i] = this.m_maxValues[i] = 0.0D / 0.0;
        }

        for(i = 0; i < this.m_theInstances.numInstances(); ++i) {
            this.updateMinMax(this.m_theInstances.instance(i));
        }

        this.doEM();
        this.m_theInstances = new Instances(this.m_theInstances, 0);
    }

    public double[] clusterPriors() {
        double[] n = new double[this.m_priors.length];
        System.arraycopy(this.m_priors, 0, n, 0, n.length);
        return n;
    }

    public double[] logDensityPerClusterForInstance(Instance inst) throws Exception {
        double[] wghts = new double[this.m_num_clusters];
        this.m_replaceMissing.input(inst);
        inst = this.m_replaceMissing.output();

        for(int i = 0; i < this.m_num_clusters; ++i) {
            double logprob = 0.0D;

            for(int j = 0; j < this.m_num_attribs; ++j) {
                if (!inst.isMissing(j)) {
                    if (inst.attribute(j).isNominal()) {
                        logprob += Math.log(this.m_model[i][j].getProbability(inst.value(j)));
                    } else {
                        logprob += this.logNormalDens(inst.value(j), this.m_modelNormal[i][j][0], this.m_modelNormal[i][j][1]);
                    }
                }
            }

            wghts[i] = logprob;
        }

        return wghts;
    }

    private void doEM() throws Exception {
        if (this.m_verbose) {
            System.out.println("Seed: " + this.getSeed());
        }

        this.m_rr = new Random((long)this.getSeed());

        int i;
        for(i = 0; i < 10; ++i) {
            this.m_rr.nextDouble();
        }

        this.m_num_instances = this.m_theInstances.numInstances();
        this.m_num_attribs = this.m_theInstances.numAttributes();
        if (this.m_verbose) {
            System.out.println("Number of instances: " + this.m_num_instances + "\nNumber of atts: " + this.m_num_attribs + "\n");
        }

        if (this.m_initialNumClusters == -1) {
            if (this.m_theInstances.numInstances() > 9) {
                this.CVClusters();
                this.m_rr = new Random((long)this.getSeed());

                for(i = 0; i < 10; ++i) {
                    this.m_rr.nextDouble();
                }
            } else {
                this.m_num_clusters = 1;
            }
        }

        this.EM_Init(this.m_theInstances);
        this.m_loglikely = this.iterate(this.m_theInstances, this.m_verbose);
    }

    private double iterate(Instances inst, boolean report) throws Exception {
        double llkold = 0.0D;
        double llk = 0.0D;
        if (report) {
            this.EM_Report(inst);
        }

        boolean ok = false;
        int seed = this.getSeed();
        int restartCount = 0;

        while(!ok) {
            try {
                for(int i = 0; i < this.m_max_iterations; ++i) {
                    llkold = llk;
                    llk = this.E(inst, true);
                    if (report) {
                        System.out.println("Loglikely: " + llk);
                    }

                    if (i > 0 && llk - llkold < 1.0E-6D) {
                        break;
                    }

                    this.M(inst);
                }

                ok = true;
            } catch (Exception var13) {
                var13.printStackTrace();
                ++seed;
                ++restartCount;
                this.m_rr = new Random((long)seed);

                for(int z = 0; z < 10; ++z) {
                    this.m_rr.nextDouble();
                    this.m_rr.nextInt();
                }

                if (restartCount > 5) {
                    --this.m_num_clusters;
                    restartCount = 0;
                }

                this.EM_Init(this.m_theInstances);
            }
        }

        if (report) {
            this.EM_Report(inst);
        }

        return llk;
    }

    public String getRevision() {
        return RevisionUtils.extract("$Revision: 6298 $");
    }

    public static void main(String[] argv) {
        runClusterer(new EM_modificada(), argv);
    }
}
