/*
 * Copyright 2026 Ruslan Kashapov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package local.mylan.service.rest.spi;

import static local.mylan.service.api.model.DeviceProtocol.NFS;
import static local.mylan.service.api.model.DeviceProtocol.SMB;
import static local.mylan.service.test.NavResourceTestUtils.assertDeviceList;
import static local.mylan.service.test.NavResourceTestUtils.device;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import local.mylan.service.api.NavResourceService;
import local.mylan.service.api.model.Device;
import local.mylan.service.rest.api.NavResourceRestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultNavResourceRestServiceTest {

    private static final Integer DEVICE_ID1 = 1;
    private static final Integer DEVICE_ID2 = 2;
    private static final String DEVICE_NAME1 = "NAME1";
    private static final String DEVICE_NAME2 = "NAME2";
    private static final String IP1 = "192.168.1.101";
    private static final String IP2 = "192.168.1.102";
    private static final String IP3 = "192.168.1.103";

    private static final Device DEVICE1 = device(DEVICE_ID1, DEVICE_NAME1, SMB, List.of(IP1), null);
    private static final Device DEVICE2 = device(DEVICE_ID2, DEVICE_NAME2, NFS, List.of(IP2), null);

    @Mock
    NavResourceService navResourceService;

    NavResourceRestService restService;

    @BeforeEach
    void beforeEach(){
        restService = new DefaultNavResourceRestService(navResourceService);
    }

    @Test
    void listDevices(){
        final var devices = List.of(DEVICE1, DEVICE2);
        doReturn(devices).when(navResourceService).getAllDevices();
        assertDeviceList(devices, restService.getDevices());
    }

}
