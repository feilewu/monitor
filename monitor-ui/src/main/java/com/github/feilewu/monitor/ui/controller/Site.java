package com.github.feilewu.monitor.ui.controller;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.feilewu.monitor.core.ui.AgentInfoCollection;
import com.github.feilewu.monitor.ui.UIContext;
import com.github.feilewu.monitor.ui.controller.dto.AgentInfoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import scala.jdk.javaapi.CollectionConverters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: pf_xu
 * @Date: 2024/5/27 22:20
 * @email：pfxuchn@gmail.com
 */
@Controller
public class Site {


    @Autowired
    private UIContext uiContext;


    @RequestMapping(value = "/", produces = "text/html;charset=utf-8")
    public String index(Model model) {
        model.addAttribute("agentList", buildAgentInfos(uiContext.getAction().agentInfos()));
        //  List<List<AgentInfoDTO>> agentInfoList = new ArrayList<>();
        //  List<AgentInfoDTO> innerList1 = new ArrayList<>();
        //  innerList1.add(new AgentInfoDTO("张三", "30"));
        //  innerList1.add(new AgentInfoDTO("李四", "35"));
        //  List<AgentInfoDTO> innerList2 = new ArrayList<>();
        //  innerList2.add(new AgentInfoDTO("王五", "40"));
        //  innerList2.add(new AgentInfoDTO("赵六", "45"));
        //  agentInfoList.add(innerList1);
        //  agentInfoList.add(innerList2);
        //  model.addAttribute("agentList", agentInfoList);
        return "index";
    }

    private List<List<AgentInfoDTO>> buildAgentInfos(AgentInfoCollection collection) {

        List<AgentInfoDTO> agentInfoDTOS = CollectionConverters.asJava(collection.toList())
                .stream()
                .map(agentInfo -> new AgentInfoDTO(agentInfo.host(), agentInfo.port()))
                .collect(Collectors.toList());

        if (agentInfoDTOS.size() % 2 != 0) {
            agentInfoDTOS.add(null);
        }

        Map<Boolean, List<AgentInfoDTO>> collect = agentInfoDTOS.stream()
                .collect(Collectors.partitioningBy(
                        agentInfoDTO -> agentInfoDTOS.indexOf(agentInfoDTO) % 2 == 0,
                        Collectors.toList()));

        List<List<AgentInfoDTO>> agents = new ArrayList<>();

        List<AgentInfoDTO> dtos1 = collect.get(false);
        List<AgentInfoDTO> dtos2 = collect.get(true);
        for (int i = 0; i< dtos1.size(); i++) {
            List<AgentInfoDTO> subList = new ArrayList<>();
            if (dtos1.get(i) != null) {
                subList.add(dtos1.get(i));
            }

            if (dtos2.get(i) != null) {
                subList.add(dtos2.get(i));
            }
            agents.add(subList);
        }

        return agents;

    }


    public static void main(String[] args) {
        List<Integer> numbers = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));

        if (numbers.size() % 2 != 0) {
            numbers.add(null);
        }

        Map<Boolean, List<Integer>> collect = numbers.stream()
                .collect(Collectors.partitioningBy(
                        agentInfoDTO -> numbers.indexOf(agentInfoDTO) % 2 == 0,
                        Collectors.toList()));

        System.out.println(collect.get(true));
        System.out.println(collect.get(false));
        System.out.println(collect.values());
    }











}
