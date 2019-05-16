/*
 * Copyright 2009-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.client.lib.domain;

import java.util.Date;
import java.util.Map;

import org.cloudfoundry.client.lib.util.CloudUtil;

public class InstanceInfo {

    private String debugIp;
    private int debugPort;
    private int index;
    private Date since;

    private InstanceState state;

    // Required by Jackson.
    public InstanceInfo() {
    }

    public InstanceInfo(Map<String, Object> infoMap) {
        since = new Date(CloudUtil.parse(Long.class, infoMap.get("since")) * 1000);
        index = CloudUtil.parse(Integer.class, infoMap.get("index"));
        String instanceState = CloudUtil.parse(String.class, infoMap.get("state"));
        state = InstanceState.valueOfWithDefault(instanceState);
        debugIp = CloudUtil.parse(String.class, infoMap.get("debug_ip"));
        debugPort = CloudUtil.parse(Integer.class, infoMap.get("debug_port"));
    }

    public String getDebugIp() {
        return debugIp;
    }

    public int getDebugPort() {
        return debugPort;
    }

    public int getIndex() {
        return index;
    }

    public Date getSince() {
        return since;
    }

    public InstanceState getState() {
        return state;
    }
}
