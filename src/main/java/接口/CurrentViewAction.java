package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.config.Config;
import com.goodwill.hdr.civ.web.service.CurrentViewService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * 类描述：当前视图Action
 * @author zhaowenkai
 * @Date 2018年8月29日
 * @modify
 * 修改记录：
 *
 */
public class CurrentViewAction extends CIVAction {

	@Autowired
	private CurrentViewService cvService;

	/**
	 * @Description
	 * 末次住院病历主诉
	 */
	public void getCVConfig() {
		String result = Config.getCIV_CURRENT_CONFIG();
		renderJson(result);
	}


	/**
	 * @Description
	 * 末次住院病历主诉
	 */
	public void getCVMain() {
		Map<String, String> result = cvService.getCVMain(patientId, visitId);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 末次住院病历现病史
	 */
	public void getCVNowHis() {
		Map<String, String> result=new HashMap<String, String>();

			result = cvService.getCVNowHis(patientId, visitId);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 末次住院诊断
	 */
	public void getCVDiag() {
		Map<String, Object> result = cvService.getCVDiag(patientId, visitId);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 末次住院体征数据，仅取近两天
	 */
	public void getCVVitalSign() {
		List<Map<String, String>> result=new ArrayList<Map<String,String>>();
		String mainDiag = getParameter("mainDiag");
        String deptCode = getParameter("dept_code");
		if(StringUtils.isNotBlank(patientId))
			result = cvService.getPatVitalSign(patientId, visitId,mainDiag,deptCode);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 获取医嘱状态名称列表
	 */
	public void getCVOrderStatusNameList() {
		List<String> result = Config.getCIV_ORDERSTATUS();
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 获取患者护理任务数据
	 */
	public void getCVPatNurseTarsk() {
		List<Map<String, String>> result=new ArrayList<Map<String,String>>();
		if(StringUtils.isNotBlank(patientId))
			result = cvService.getPatNurseTask(patientId, visitId);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 获取患者风险评估数据
	 */
	public void getCVPatRiskAssess() {
		Page<Map<String, String>> result=new Page<Map<String,String>>();
		if(StringUtils.isNotBlank(patientId))
			result = cvService.getPatRiskAssess(patientId, visitId, pageNo, pageSize);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 *  获取患者诊疗计划，个性化方法，之后再扩展修改
	 */
	public void getCVPatAssessPlan() {
		Map<String, String> result = new HashMap<String, String>();
		result.put("result", cvService.getPatAssessPlan(patientId, visitId));
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 院感
	 */
	public void getPatInfectionWarn() {
		Page<Map<String, String>> result=new Page<Map<String,String>>();
		//院感数据很少，暂时通过100查询所有的院感
		pageNo = 1;
		pageSize = 100;
		if(StringUtils.isNotBlank(patientId))
			result = cvService.getPatInfectionWarn(patientId, visitId, pageNo, pageSize);
		renderJson(JsonUtil.getJSONString(result));
	}
	/**
	 * @Description
	 * 获取患者危急值数据
	 */
	public void getPatCriticalValues() {
		Page<Map<String, String>> result=new Page<Map<String,String>>();
		pageNo = 1;
		pageSize = 100;
		if(StringUtils.isNotBlank(patientId))
			result = cvService.getPatCriticalValues(patientId, visitId, pageNo, pageSize);
		renderJson(JsonUtil.getJSONString(result));
	}

}
