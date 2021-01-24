package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.config.Config;
import com.goodwill.hdr.civ.web.service.AllergyService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * @Description
 * 类描述：过敏信息Action
 * @author zhaowenkai
 * @Date 2018年9月28日
 * @modify
 * 修改记录：
 *
 */
public class AllergyAction extends CIVAction {

	private static final long serialVersionUID = 1L;

	@Autowired
	private AllergyService allergyService;

	/**
	 * @Description
	 * 某次就诊的过敏记录
	 */
	public void getAllergyList() {
		//过敏类型  类型编码
		String allergyType = getParameter("allergyType");
		//过敏程度
		String allergySeverity = getParameter("allergyLevel");
		//过敏程度数据匹配的列字段名
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
		Page<Map<String, String>> result = allergyService.getAllergyList(patientId, visitId, visitType, orderBy,
				orderDir, pageNo, pageSize, filterString);
		renderJson(JsonUtil.getJSONString(result));
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
