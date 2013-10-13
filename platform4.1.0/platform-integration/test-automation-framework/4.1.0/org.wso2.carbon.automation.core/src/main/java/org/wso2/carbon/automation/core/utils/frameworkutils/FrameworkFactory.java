package org.wso2.carbon.automation.core.utils.frameworkutils;

import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.AMSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.AsSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.Axis2Setter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.BamSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.BpsSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.BrsSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.CepSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.ClusterSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.DssSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.EsbSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.GregSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.GsSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.IsSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.ManagerSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.MbSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.MsSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productsetters.SsSetter;

public class FrameworkFactory {
    static FrameworkProperties properties = new FrameworkProperties();

    public static FrameworkProperties getFrameworkProperties(String product) {
        if (product.equals(ProductConstant.BPS_SERVER_NAME)) {
            BpsSetter bpsSetter = new BpsSetter();
            properties.setDataSource(bpsSetter.getDataSource());
            properties.setEnvironmentSettings(bpsSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(bpsSetter.getEnvironmentVariables());
            properties.setRavana(bpsSetter.getRavana());
            properties.setSelenium(bpsSetter.getSelenium());
            properties.setProductVariables(bpsSetter.getProductVariables());
            properties.setWorkerVariables(bpsSetter.getWorkerVariables());
        }

        if (product.equals(ProductConstant.AXIS2_SERVER_NAME)) {
            Axis2Setter axis2Setter = new Axis2Setter();
            properties.setDataSource(axis2Setter.getDataSource());
            properties.setEnvironmentSettings(axis2Setter.getEnvironmentSettings());
            properties.setEnvironmentVariables(axis2Setter.getEnvironmentVariables());
            properties.setRavana(axis2Setter.getRavana());
            properties.setSelenium(axis2Setter.getSelenium());
            properties.setProductVariables(axis2Setter.getProductVariables());
        }

        if (product.equals(ProductConstant.APP_SERVER_NAME)) {

            AsSetter asSetter = new AsSetter();
            properties.setDataSource(asSetter.getDataSource());
            properties.setEnvironmentSettings(asSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(asSetter.getEnvironmentVariables());
            properties.setRavana(asSetter.getRavana());
            properties.setSelenium(asSetter.getSelenium());
            properties.setProductVariables(asSetter.getProductVariables());
            properties.setWorkerVariables(asSetter.getWorkerVariables());
        }

        if (product.equals(ProductConstant.AM_SERVER_NAME)) {

            AMSetter amSetter = new AMSetter();
            properties.setDataSource(amSetter.getDataSource());
            properties.setEnvironmentSettings(amSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(amSetter.getEnvironmentVariables());
            properties.setRavana(amSetter.getRavana());
            properties.setSelenium(amSetter.getSelenium());
            properties.setProductVariables(amSetter.getProductVariables());
            properties.setWorkerVariables(amSetter.getWorkerVariables());
        }


        if (product.equals(ProductConstant.ESB_SERVER_NAME)) {
            EsbSetter esbSetter = new EsbSetter();
            properties.setDataSource(esbSetter.getDataSource());
            properties.setEnvironmentSettings(esbSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(esbSetter.getEnvironmentVariables());
            properties.setRavana(esbSetter.getRavana());
            properties.setSelenium(esbSetter.getSelenium());
            properties.setProductVariables(esbSetter.getProductVariables());
            properties.setWorkerVariables(esbSetter.getWorkerVariables());
        }


        if (product.equals(ProductConstant.DSS_SERVER_NAME)) {
            DssSetter dssSetter = new DssSetter();
            properties.setDataSource(dssSetter.getDataSource());
            properties.setEnvironmentSettings(dssSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(dssSetter.getEnvironmentVariables());
            properties.setRavana(dssSetter.getRavana());
            properties.setSelenium(dssSetter.getSelenium());
            properties.setProductVariables(dssSetter.getProductVariables());
            properties.setWorkerVariables(dssSetter.getWorkerVariables());
        }

        if (product.equals(ProductConstant.SS_SERVER_NAME)) {
            SsSetter ssSetter = new SsSetter();
            properties.setDataSource(ssSetter.getDataSource());
            properties.setEnvironmentSettings(ssSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(ssSetter.getEnvironmentVariables());
            properties.setRavana(ssSetter.getRavana());
            properties.setSelenium(ssSetter.getSelenium());
            properties.setProductVariables(ssSetter.getProductVariables());
            properties.setWorkerVariables(ssSetter.getWorkerVariables());
        }

        if (product.equals(ProductConstant.IS_SERVER_NAME)) {
            IsSetter isSetter = new IsSetter();
            properties.setDataSource(isSetter.getDataSource());
            properties.setEnvironmentSettings(isSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(isSetter.getEnvironmentVariables());
            properties.setRavana(isSetter.getRavana());
            properties.setSelenium(isSetter.getSelenium());
            properties.setProductVariables(isSetter.getProductVariables());
            properties.setWorkerVariables(isSetter.getWorkerVariables());
        }

        if (product.equals(ProductConstant.BRS_SERVER_NAME)) {
            BrsSetter brsSetter = new BrsSetter();
            properties.setDataSource(brsSetter.getDataSource());
            properties.setEnvironmentSettings(brsSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(brsSetter.getEnvironmentVariables());
            properties.setRavana(brsSetter.getRavana());
            properties.setSelenium(brsSetter.getSelenium());
            properties.setProductVariables(brsSetter.getProductVariables());
            properties.setWorkerVariables(brsSetter.getWorkerVariables());
        }

        if (product.equals(ProductConstant.CEP_SERVER_NAME)) {
            CepSetter cepSetter = new CepSetter();
            properties.setDataSource(cepSetter.getDataSource());
            properties.setEnvironmentSettings(cepSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(cepSetter.getEnvironmentVariables());
            properties.setRavana(cepSetter.getRavana());
            properties.setSelenium(cepSetter.getSelenium());
            properties.setProductVariables(cepSetter.getProductVariables());
            properties.setWorkerVariables(cepSetter.getWorkerVariables());
        }

        if (product.equals(ProductConstant.GREG_SERVER_NAME)) {
            GregSetter gregSetter = new GregSetter();
            properties.setDataSource(gregSetter.getDataSource());
            properties.setEnvironmentSettings(gregSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(gregSetter.getEnvironmentVariables());
            properties.setRavana(gregSetter.getRavana());
            properties.setSelenium(gregSetter.getSelenium());
            properties.setProductVariables(gregSetter.getProductVariables());
            properties.setWorkerVariables(gregSetter.getWorkerVariables());
        }

        if (product.equals(ProductConstant.GS_SERVER_NAME)) {
            GsSetter gsSetter = new GsSetter();
            properties.setDataSource(gsSetter.getDataSource());
            properties.setEnvironmentSettings(gsSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(gsSetter.getEnvironmentVariables());
            properties.setRavana(gsSetter.getRavana());
            properties.setSelenium(gsSetter.getSelenium());
            properties.setProductVariables(gsSetter.getProductVariables());
            properties.setWorkerVariables(gsSetter.getWorkerVariables());
        }

        if (product.equals(ProductConstant.MB_SERVER_NAME)) {
            MbSetter mbSetter = new MbSetter();
            properties.setDataSource(mbSetter.getDataSource());
            properties.setEnvironmentSettings(mbSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(mbSetter.getEnvironmentVariables());
            properties.setRavana(mbSetter.getRavana());
            properties.setSelenium(mbSetter.getSelenium());
            properties.setProductVariables(mbSetter.getProductVariables());
            properties.setWorkerVariables(mbSetter.getWorkerVariables());
        }

        if (product.equals(ProductConstant.MS_SERVER_NAME)) {
            MsSetter msSetter = new MsSetter();
            properties.setDataSource(msSetter.getDataSource());
            properties.setEnvironmentSettings(msSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(msSetter.getEnvironmentVariables());
            properties.setRavana(msSetter.getRavana());
            properties.setSelenium(msSetter.getSelenium());
            properties.setProductVariables(msSetter.getProductVariables());
            properties.setWorkerVariables(msSetter.getWorkerVariables());
        }

        if (product.equals(ProductConstant.BAM_SERVER_NAME)) {
            BamSetter bamSetter = new BamSetter();
            properties.setDataSource(bamSetter.getDataSource());
            properties.setEnvironmentSettings(bamSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(bamSetter.getEnvironmentVariables());
            properties.setRavana(bamSetter.getRavana());
            properties.setSelenium(bamSetter.getSelenium());
            properties.setProductVariables(bamSetter.getProductVariables());
            properties.setWorkerVariables(bamSetter.getWorkerVariables());
        }


        if (product.equals(ProductConstant.MANAGER_SERVER_NAME)) {
            ManagerSetter manSetter = new ManagerSetter();
            properties.setDataSource(manSetter.getDataSource());
            properties.setEnvironmentSettings(manSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(manSetter.getEnvironmentVariables());
            properties.setRavana(manSetter.getRavana());
            properties.setSelenium(manSetter.getSelenium());
            properties.setProductVariables(manSetter.getProductVariables());
            properties.setWorkerVariables(manSetter.getWorkerVariables());
        }

        if (product.equals(ProductConstant.CLUSTER)) {
            ClusterSetter clusterSetter = new ClusterSetter();
            properties.setDataSource(clusterSetter.getDataSource());
            properties.setEnvironmentSettings(clusterSetter.getEnvironmentSettings());
            properties.setEnvironmentVariables(clusterSetter.getEnvironmentVariables());
            properties.setRavana(clusterSetter.getRavana());
            properties.setSelenium(clusterSetter.getSelenium());
            //properties.setProductVariables(clusterSetter.getProductVariables(cluster));
        }

        return properties;
    }

    public static FrameworkProperties getClusterProperties(String cluster) {

        ClusterSetter clusterSetter = new ClusterSetter();
        properties.setDataSource(clusterSetter.getDataSource());
        properties.setEnvironmentSettings(clusterSetter.getEnvironmentSettings());
        properties.setEnvironmentVariables(clusterSetter.getEnvironmentVariables());
        properties.setRavana(clusterSetter.getRavana());
        properties.setSelenium(clusterSetter.getSelenium());
        properties.setProductVariables(clusterSetter.getProductVariables(cluster));
        return properties;
    }

}
