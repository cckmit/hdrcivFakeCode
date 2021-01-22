package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.config.Config;
import com.goodwill.hdr.civ.web.service.PatientListService;
import com.goodwill.security.utils.SecurityUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * @Description
 * 类描述：患者列表
 * @author zhangsen
 * @Date 2018年6月11日
 * @modify
 * 修改记录：
 *
 */
public class PatientListAction extends CIVAction {

	private static final long serialVersionUID = -3688666558706749090L;

	@Autowired
	private PatientListService patientListService;

	/**
	 * @Description
	 * 患者列表 卡片式
	 */
	public void getPatientList_Card() {
		//获取患者列表查询条件（患者姓名、就诊号、就诊科室、就诊开始日期、就诊结束日期、第几页、每页个数）
		int pageNo =0;
		int pageSize=15;
		String patientName = getParameter("patientName");
		String visitId = getParameter("visitID");
		String visitDept = getParameter("visitDept");
		String visitBTime = getParameter("visitBTime");
		String visitETime = getParameter("visitETime");
		if(StringUtils.isNotBlank(getParameter("pageNo"))){
			pageNo = Integer.parseInt(getParameter("pageNo"));
		}
		if(StringUtils.isNotBlank(getParameter("pageSize"))){
			pageSize = Integer.parseInt(getParameter("pageSize"));
		}
		Page<Map<String,String>> list= patientListService.getPatientList_Card(patientName,
				visitId, visitDept, visitBTime, visitETime, pageNo, pageSize);

		renderJson(JsonUtil.getJSONString(list));

	}

	/**
	 * @Description
	 * 患者列表 列表样式
	 */
	public void getPatientList_List() {

		int pageNo =0;
		int pageSize=10;
		String patientId = getParameter("patientId");
		String visitNo = getParameter("visitId");
		String patientName = getParameter("patientName");
		String card_Id = getParameter("card_Id");
		String visit_Type = getParameter("visit_Type");
		String visit_Dept = getParameter("visitDept");
		String visitBTime = getParameter("visitBTime");
		String visitETime = getParameter("visitETime");
		String dischargeBTime = getParameter("dischargeBTime");
		String dischargeETime = getParameter("dischargeETime");
		String district = getParameter("district");
		if(StringUtils.isNotBlank(getParameter("pageNo"))){
			pageNo = Integer.parseInt(getParameter("pageNo"));
		}
		if(StringUtils.isNotBlank(getParameter("pageSize"))){
			pageSize = Integer.parseInt(getParameter("pageSize"));
		}

		Page<Map<String,String>> list= patientListService.getPatientList_List(patientId, visitNo,
				patientName, card_Id, visit_Type, visit_Dept, visitBTime, visitETime,dischargeBTime,dischargeETime,district, pageNo, pageSize);

		renderJson(JsonUtil.getJSONString(list));
	}

	/**
	 * @Description
	 * 患者列表  获取科室列表
	 */
	public void getUserDeptAuth(){
		String keyWord = getParameter("keyWord");
		int pageNo = Integer.parseInt(getParameter("pageNo"));
		int pageSize = Integer.parseInt(getParameter("pageSize"));

		String user= SecurityUtils.getCurrentUser().getUsername();
		if(!"admin".equals(user)){
			Page<Map<String, String>> page= patientListService.getUserDeptAuth(keyWord, pageNo, pageSize);
			renderJson(JsonUtil.getJSONString(page));
		}else{
			Page<Map<String, String>> page= patientListService.getAllDept(keyWord, pageNo, pageSize);
			renderJson(JsonUtil.getJSONString(page));
		}
	}



	/**
	 * @Description
	 * 获取所有病区
	 */
	public void getAllDept(){
		String keyWord = getParameter("keyWord");
		String typeCode = getParameter("typeCode");
		int pageNo = Integer.parseInt(getParameter("pageNo"));
		int pageSize = Integer.parseInt(getParameter("pageSize"));
		Page<Map<String, String>> page= patientListService.getAllSolrFacetData(keyWord,typeCode, "DEPT_ADMISSION_TO_CODE","DEPT_ADMISSION_TO_NAME",pageNo, pageSize);
		renderJson(JsonUtil.getJSONString(page));
	}

	/**
	 * @Description
	 * 获取所有部门
	 */
	public void getAllDistricts(){
		String keyWord = getParameter("keyWord");
		String typeCode = getParameter("typeCode");
		int pageNo = Integer.parseInt(getParameter("pageNo"));
		int pageSize = Integer.parseInt(getParameter("pageSize"));
		Page<Map<String, String>> page= patientListService.getAllSolrFacetData(keyWord,typeCode, "DISTRICT_ADMISSION_TO_CODE","DISTRICT_ADMISSION_TO_NAME",pageNo, pageSize);
		renderJson(JsonUtil.getJSONString(page));
	}

	/**
	 * 患者列表从solr进行fact字段统计
	 */
	public void getFactData(){
		String keyWord = getParameter("keyWord");
		String typeCode = getParameter("typeCode");
		//dept 科室统计   districts 病区
		String factType = getParameter("factType");
		String factCode = "";
		String factName = "";
		if ("dept".equals(factType)) {
			factCode = "DEPT_ADMISSION_TO_CODE";
			factName = "DEPT_ADMISSION_TO_NAME";
		} else if ("districts".equals(factType)) {
			factCode = "DISTRICT_ADMISSION_TO_CODE";
			factName = "DISTRICT_ADMISSION_TO_NAME";
		}
		int pageNo = Integer.parseInt(getParameter("pageNo"));
		int pageSize = Integer.parseInt(getParameter("pageSize"));
		Page<Map<String, String>> page= patientListService.getAllSolrFacetData(keyWord,typeCode,factCode,factName,pageNo, pageSize);
		renderJson(JsonUtil.getJSONString(page));
	}

	/**
	 * @Description
	 * 列表字段
	 */
	public void getListColumn(){
		renderJson(Config.getCIV_PATIENT_COLUMN());
	}

	/**
	 *  患者列表查询条件显示配置
	 */
	public void  getPatListQueryConfig(){
		Map<String,Object> config = patientListService.getPatListQueryViewConfig();
		renderJson(JsonUtil.getJSONString(config));
	}
}
