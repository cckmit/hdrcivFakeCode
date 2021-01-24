package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.config.Config;
import com.goodwill.hdr.civ.web.service.SpecialtyViewService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * @Description
 * 类描述：专科视图
 * @author zhangsen
 * @Date 2018年4月11日
 * @modify
 * 修改记录：
 */
public class SpecialtyViewAction extends CIVAction{

	private static final long serialVersionUID = 1L;
	
	@Autowired
	private SpecialtyViewService specialtyViewService;
	
	/**
	 * @Description
	 * 专科就诊列表
	 */
	public void getSpecialtyVisitList() {
		String deptCode = getParameter("deptCode");

		List<Map<String, Object>> map=specialtyViewService.getVisitList(patientId, deptCode);
		renderJson(JsonUtil.getJSONString(map));
	}
	
	/**
	 * @Description
	 * 专科视图检、用药、检验Tab配置
	 */
	public void getViewConfig() {
		String config=Config.getSPECIAL_CUSTOM_TABLE();
		renderJson(config);
	}
	
	/**
	 * @Description
	 * 获取检验小项列表
	 */
	public void getLabSubList() {
		String deptCode = getParameter("deptCode");
		
		List<Map<String, Object>> map=specialtyViewService.getLabSubList(
				patientId,visitId, deptCode);
		renderJson(JsonUtil.getJSONString(map));
	}
	
	/**
	 * @Description
	 * 获取检验小项详情
	 */
	public void getLabSubDetails() {
		String deptCode = getParameter("deptCode");
		int dateType = Integer.valueOf(getParameter("dateType"));
		String startDate = getParameter("startDate");
		String endDate = getParameter("endDate");
		String subItemCode = getParameter("code");
		
		Map<String, Object> map=specialtyViewService.getLabSubDetails(patientId, 
				dateType, startDate, endDate, subItemCode);
		renderJson(JsonUtil.getJSONString(map));
	}
	
	/**
	 * @Description
	 * 检查列表
	 */
	public void getExamList() {
		String deptCode = getParameter("deptCode");
		
		List<Map<String, String>> list=specialtyViewService.getExamList(patientId, visitId, deptCode);
		renderJson(JsonUtil.getJSONString(list));
	}
	
	/**
	 * @Description
	 * 检查报告详情
	 */
	public void getExamDetails() {
		String deptCode = getParameter("deptCode");
		String examItemName = getParameter("name");
		
		Page<Map<String, String>> page=specialtyViewService.getExamDetails(patientId,
				deptCode, examItemName, pageSize, pageNo);
		renderJson(JsonUtil.getJSONString(page));
	}
	
	/**
	 * @Description
	 * 用药列表
	 */
	public void getDrugList() {
		String deptCode = getParameter("deptCode");
	
		List<Map<String, String>> list=specialtyViewService.getDrugList(patientId, visitId, deptCode);
		renderJson(JsonUtil.getJSONString(list));
	}
	
	/**
	 * @Description
	 * 用药详情
	 */
	public void getDrugDetails() {
		String orderItemCode = getParameter("code");
		String dateType = getParameter("dateType");
		String startDate = getParameter("startDate");
		String endDate = getParameter("endDate");
		
		Map<String, Object> map=specialtyViewService.getDrugDetails(patientId, orderItemCode, dateType, startDate, endDate);
		renderJson(JsonUtil.getJSONString(map));
	}
}
