package com.goodwill.hdr.civ.controller;

import org.springframework.web.bind.annotation.RestController;

/**
 * 首页展示患者列表
 *
 * <p>通过{@link getListColumn()}来展示就诊列表字段，
 * 通过{@link getPatientList_List()}来展示患者就诊列表。
 * </p>
 *
 * @author yutao
 * @date 2020.12.18
 */
@RestController
public class PatientListController {


    /**
     * 获取首页中患者列表展示内容
     */
//  public viod getPatientList_List()
//    获取patientId
//    获取visitId
//    获取patientName
//    获取card_Id
//    获取visit_Type
//    获取visitDept
//    获取visitBTime
//    获取visitETime
//    获取district
//    获取pageNo
//    获取pageSize
/*
调用service接口，将结果封装在page中，以json格式发送
  Page<Map<String,String>> list= patientListService.getPatientList_List(patientId, visitNo,
            patientName, card_Id, visit_Type, visit_Dept, visitBTime, visitETime,district, pageNo, pageSize);
*/


    /**
     *获取列表字段
     */

//    public void getListColumn()
//       调用Config.getCIV_PATIENT_COLUMN()获取列表字段，从mysql的config表中的CIV_PATIENT_COLUMN查询字段

}
