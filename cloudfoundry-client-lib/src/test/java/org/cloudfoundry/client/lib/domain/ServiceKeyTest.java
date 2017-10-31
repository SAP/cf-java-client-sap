package org.cloudfoundry.client.lib.domain;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class ServiceKeyTest {

    @Test
    public void testConstructorFull() {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("<paramKey>", "<paramValue>");
        Map<String, Object> credentials = new HashMap<String, Object>();
        credentials.put("<credentialKey>", "<credentialValue>");
        CloudService service = new CloudService();
        ServiceKey serviceKey = new ServiceKey(CloudEntity.Meta.defaultMeta(), "<name>", parameters, credentials, service);

        Assert.assertEquals("<name>", serviceKey.getName());
        Assert.assertEquals(parameters, serviceKey.getParameters());
        Assert.assertEquals(credentials, serviceKey.getCredentials());
        Assert.assertEquals(service, serviceKey.getService());
    }

    @Test
    public void testConstructorNameOnly() {
        ServiceKey serviceKey = new ServiceKey(CloudEntity.Meta.defaultMeta(), "<name>");

        Assert.assertEquals("<name>", serviceKey.getName());
        Assert.assertNull(serviceKey.getParameters());
        Assert.assertNull(serviceKey.getCredentials());
        Assert.assertNull(serviceKey.getService());
    }

    @Test
    public void testSetCloudService() {
        ServiceKey serviceKey = new ServiceKey(CloudEntity.Meta.defaultMeta(), "<name>");

        Assert.assertNull(serviceKey.getService());

        CloudService service = new CloudService();
        serviceKey.setService(service);
        Assert.assertEquals(service, serviceKey.getService());
    }
}
