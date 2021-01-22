package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.web.entity.TimeAxisConfig;
import com.goodwill.hdr.civ.web.service.TimeAxisViewService;
import com.goodwill.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * 类描述：时间轴
 * @author zhangsen
 * @Date 2018年4月11日
 * @modify
 * 修改记录：
 */
public class TimeAxisViewAction extends CIVAction{

	private static final long serialVersionUID = 1L;

	@Autowired
	private TimeAxisViewService axisViewService;

	/**
	 * @Description
	 * 就诊列表
	 */
	public void getVisitDeptList(){
		String deptName = getParameter("keyWord");

		Page<Map<String, String>> page=axisViewService.getVisitDeptList(patientId,deptName,pageSize,pageNo);
		renderJson(JsonUtil.getJSONString(page));
	}

	/**
	 * @Description
	 * 就诊周数列表
	 */
	public void getVisitTimeList(){
		List<Map<String, String>> map=axisViewService.getVisitTimeList(patientId,visitId);
		renderJson(JsonUtil.getJSONString(map));
	}

	/**
	 * @Description
	 * 获取部分生命体征
	 */
	public void getVisitTimeTpList(){
		String admissionTime = getParameter("time");
		String weekDays=getParameter("week");
		int week=0;
		if(!"all".equals(weekDays))
			week = Integer.valueOf(getParameter("week"));
		int cols = Integer.valueOf(getParameter("cols"));
		 List<Map<String, Object>> rs=axisViewService.getVisitTimeTpList(patientId, visitId, admissionTime, week,cols);
		renderJson(JsonUtil.getJSONString(rs));
	}

	/**
	 * @Description
	 * 获取手术
	 */
	public void getVisitOperTimeList(){
		String admissionTime = getParameter("time");
		String weekDays=getParameter("week");
		int week=0;
		if(!"all".equals(weekDays))
			week = Integer.valueOf(getParameter("week"));
		int cols = Integer.valueOf(getParameter("cols"));
		List<Map<String, Object>> rs=axisViewService.getVisitOperTimeList(patientId, visitId, admissionTime, week,cols);
		renderJson(JsonUtil.getJSONString(rs));
	}

	/**
	 * @Description
	 *体温和脉搏
	 */
	public void getVTBloodAndPulse(){
		String admissionTime = getParameter("time");
		String weekDays=getParameter("week");
		int week=0;
		if(!"all".equals(weekDays))
			week = Integer.valueOf(getParameter("week"));
		int cols = Integer.valueOf(getParameter("cols"));
		Map<String, Object> rs=axisViewService.getVisitTimeBloodAndPulse(patientId, visitId, admissionTime, week,cols);
		renderJson(JsonUtil.getJSONString(rs));
	}

	/**
	 * @Description
	 * 药品
	 */
	public void getVisitTimeDrugList(){
		String admissionTime = getParameter("time");
		String weekDays=getParameter("week");
		//末次诊断
		String mainDiag =getParameter("mainDiagCode");
		//末次科室
		String deptCode =getParameter("deptCode");
		//是否显示重点药品
		String  showMainIndicator = getParameter("showMainIndicator");
		int week=0;
		if(!"all".equals(weekDays))
			week = Integer.valueOf(getParameter("week"));
		int cols = Integer.valueOf(getParameter("cols"));
		Map<String, Object> map=axisViewService.getVisitTimeDrugList(patientId, visitId,admissionTime,mainDiag,deptCode,showMainIndicator, week ,cols);
		renderJson(JsonUtil.getJSONString(map));
	}

	/**
	 * @Description
	 * 检验
	 */
	public void getVisitTimeLabList(){
		String admissionTime = getParameter("time");
		String weekDays=getParameter("week");
		int week=0;
		//末次诊断
		String mainDiag =getParameter("mainDiagCode");
		//末次科室
		String deptCode =getParameter("deptCode");
		//是否显示重点药品
		String  showMainIndicator = getParameter("showMainIndicator");
        if(null == showMainIndicator || "0".equals(showMainIndicator)){
			mainDiag = "";
			deptCode = "";
		}
		if(!"all".equals(weekDays))
			week = Integer.valueOf(getParameter("week"));
		int cols = Integer.valueOf(getParameter("cols"));
		Map<String, Object> map=axisViewService.getVisitTimeLabList(patientId, visitId, admissionTime,mainDiag,deptCode, week,cols);
		renderJson(JsonUtil.getJSONString(map));
	}

	/**
	 * @Description
	 * 检查
	 */
	public void getVisitTimeExamList(){
		String admissionTime = getParameter("time");
		String weekDays=getParameter("week");
		int week=0;
		//末次诊断
		String mainDiag =getParameter("mainDiagCode");
		//末次科室
		String deptCode =getParameter("deptCode");
		//是否显示重点药品
		String  showMainIndicator = getParameter("showMainIndicator");
		if(null == showMainIndicator || "0".equals(showMainIndicator)){
			mainDiag = "";
			deptCode = "";
		}
		if(!"all".equals(weekDays))
			week = Integer.valueOf(getParameter("week"));
		int cols = Integer.valueOf(getParameter("cols"));
		Map<String, Object> map=axisViewService.getVisitTimeExamList(patientId, visitId, admissionTime,mainDiag, deptCode,week,cols);
		renderJson(JsonUtil.getJSONString(map));
	}

	/**
	 * @Description
	 * 诊疗记录
	 */
	public void getVisitTimeClinicList(){
		String admissionTime = getParameter("time");
		String weekDays=getParameter("week");
		int week=0;

		if(!"all".equals(weekDays))
			week = Integer.valueOf(getParameter("week"));
		int cols = Integer.valueOf(getParameter("cols"));
		Map<String, Object> map=axisViewService.getVisitTimeClinicList(patientId, visitId, admissionTime, week,cols);
		renderJson(JsonUtil.getJSONString(map));
	}

	/**
	 * @Description
	 *手术
	 */
	public void getVisitTimeOperList(){
		String admissionTime = getParameter("time");
		String weekDays=getParameter("week");
		int week=0;
		if(!"all".equals(weekDays))
			week = Integer.valueOf(getParameter("week"));
		int cols = Integer.valueOf(getParameter("cols"));
		Map<String, Object> map=axisViewService.getVisitTimeOperList(patientId, admissionTime, week,cols);
		renderJson(JsonUtil.getJSONString(map));
	}

	/**
	 * @Description
	 * 治疗相关
	 */
	public void getVisitTimeCureList() {
		String admissionTime = getParameter("time");
		String weekDays=getParameter("week");
		int week=0;

		//末次诊断
		String mainDiag =getParameter("mainDiagCode");
		//末次科室
		String deptCode =getParameter("deptCode");
		//是否显示重点药品
		String  showMainIndicator = getParameter("showMainIndicator");
		if(null == showMainIndicator || "0".equals(showMainIndicator)){
			mainDiag = "";
			deptCode = "";
		}
//		mainDiag = "I10xx02";
//		deptCode = "1150101";
		if(!"all".equals(weekDays))
			week = Integer.valueOf(getParameter("week"));
		int cols = Integer.valueOf(getParameter("cols"));
		Map<String, Object> map = axisViewService.getVisitTimeCureList(patientId, visitId, admissionTime,mainDiag,deptCode, week,cols);
		renderJson(JsonUtil.getJSONString(map));
	}

	/**
	 * @Description
	 * 入出转
	 */
	public void getVisitTimePat_AdtList() {
		//String admissionTime = getParameter("time");
		//int week = Integer.valueOf(getParameter("week"));
		List<Map<String, Object>> map = axisViewService.getVisitTimePat_AdtList(patientId, visitId);
		renderJson(JsonUtil.getJSONString(map));
	}

	/**
	 * @Description
	 * 新增配置
	 */
	public void insertConfig() {
		Map<String, String> rs=new HashMap<String, String>();
		//usercode,patientid,visittype,visitid,itemcode,itemname,subitemcode,subitemname
		String usercode = SecurityUtils.getCurrentUser().getUsername();
		String itemcode = getParameter("itemCode");
		String itemname = getParameter("itemName");
		String subitemcode = getParameter("subItemCode");
		String subitemname = getParameter("subItemName");
		TimeAxisConfig config=new TimeAxisConfig();
		config.setUsercode(usercode);
		config.setPatientid(patientId);
		config.setVisittype(visitType);
		config.setVisitid(visitId);
		config.setItemcode(itemcode);
		config.setItemname(itemname);
		config.setSubitemcode(subitemcode);
		config.setSubitemname(subitemname);

		rs=axisViewService.insertConfig(config);
		renderJson(JsonUtil.getJSONString(rs));
	}

	/**
	 * @Description
	 * 获取检验新增配置
	 */
	public void getConfigs() {
		List<Map<String, String>> rs=new ArrayList<Map<String,String>>();
		String usercode = SecurityUtils.getCurrentUser().getUsername();
		//末次诊断
		String mainDiag =getParameter("mainDiagCode");
		//末次科室
		String deptCode =getParameter("deptCode");
		//是否显示重点药品
		String  showMainIndicator = getParameter("showMainIndicator");
		if(null == showMainIndicator || "0".equals(showMainIndicator)){
			rs = axisViewService.getConfigList(usercode, patientId, visitType, visitId);
		}else{
			rs = axisViewService.getConfigConfigList(mainDiag,deptCode);
		}
		renderJson(JsonUtil.getJSONString(rs));
	}

	/**
	 * @Description
	 * 删除配置
	 */
	public void deleteConfig() {
		Map<String, String> rs=new HashMap<String, String>();
		String id = getParameter("id");

		boolean b=axisViewService.deleteConfig(id);
		if(b){
			rs.put("result", "1");
		}else{
			rs.put("result", "0");
		}
		renderJson(JsonUtil.getJSONString(rs));
	}
	/**
	 * @Description
	 * 删除配置
	 */
	public void getInspectItemLine() {
		Map<String, Object> rs=new HashMap<String, Object>();
		String subitemcode = getParameter("subItemCode");
		String admissionTime = getParameter("time");
		String weekDays=getParameter("week");
		int week=0;
		if(!"all".equals(weekDays))
			week = Integer.valueOf(getParameter("week"));
		int cols = Integer.valueOf(getParameter("cols"));
		rs=axisViewService.getInspectItemLine(patientId, "02", visitId, subitemcode, admissionTime, week,cols);
		renderJson(JsonUtil.getJSONString(rs));
	}

}
