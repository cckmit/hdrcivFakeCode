package 接口;


import 逻辑.AllergyService;

import java.util.List;
import java.util.Map;
d
/**
 *
 * 类描述：过敏信息
 * @author 余涛
 * @date 2021/1/22
 *
 */
public class AllergyAction  {



	private AllergyService allergyService;

	/**
	 *  某次就诊的过敏记录
	 * @param patientId 患者id
	 * @param visitId 就诊次数标识
	 * @param visitType 就诊类型
	 * @param orderBy 排序字段
	 * @param orderDir 排序规则
	 * @param pageNo 页码
	 * @param pageSize 每页大小
	 * @param allergyType 过敏类型
	 */
	public void getAllergyList(String patientId, String visitId, String visitType, String orderBy,
							   String orderDir, int pageNo, int pageSize,String allergyType) {


	/*如果过敏类型不为空，拼接字符串 "ALLERGY_CATEGORY_CODE|=|" + allergyType 作为过滤条件；*/
	String	filterString = "ALLERGY_CATEGORY_CODE|=|" + allergyType;
    /*将拼接后的字符串连同其他参数传给逻辑层*/
		allergyService.getAllergyList(patientId, visitId, visitType, orderBy,
				orderDir, pageNo, pageSize, filterString);


	}

	/**
	 * @Description
	 * 所有的过敏信息
	 */
	public void getAllergys() {
		//过敏类型  类型编码
		String allergyType = getParameter("allergyType");
		//过敏程度
		String allergySeverity = getParameter("allergyLevel");
		String allergyFieldName = Config.getAllergySeverityFieldName();
		String filterString = "";
		if (StringUtils.isNotBlank(allergyType)) {
			filterString = "ALLERGY_CATEGORY_CODE|=|" + allergyType;
		}
		if (filterString.length()>0) {
			if (StringUtils.isNotBlank(allergySeverity)) {
				filterString += ";"+allergyFieldName+"|=|" + allergySeverity;
			}
		}else if (StringUtils.isNotBlank(allergySeverity)){
			filterString = allergyFieldName+"|=|" + allergySeverity;
		}
		//关联患者号
		String outPatientId = getParameter("outPatientId");

		Page<Map<String, String>> result = allergyService.getAllergys(patientId, outPatientId, orderBy, orderDir,
				pageNo, pageSize, filterString);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 获取过敏类型
	 */
	public void getAllergyTypes() {
		List<Map<String, String>> result = allergyService.getAllergyTypes();
		renderJson(JsonUtil.getJSONString(result));
	}
	/**
	 * @Description
	 * 获取过敏程度
	 */
	public void  getAllergySeverity(){
		List<Map<String, String>> allergySeverity = allergyService.getAllergySeverity();
		renderJson(JsonUtil.getJSONString(allergySeverity));
	}

}
