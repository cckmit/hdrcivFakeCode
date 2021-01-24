package 接口;

import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.web.service.OCLService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 医嘱闭环Action
 * @author peijiping
 * @Date 2018年4月25日
 */
public class OrderCloseLoopAction extends CIVAction {

	//闭环远程调用
	@Autowired
	private OCLService oclService;

	/**
	 * @Description
	 * 口服药和静脉药闭环
	 */
	public void drugOCL() {
		//医嘱编号
		String orderno = getParameter("orderNo");
		if (StringUtils.isBlank(patientId) || StringUtils.isBlank(visitId)) {
			return;
		}
		//获取结果数据
		String result = oclService.getDrugOCL(patientId, visitId, orderno);
		renderJson(result);
	}

	/**
	 * @Description
	 * 检验医嘱闭环
	 */
	public void labOCL() {
//		String field = Config.getOrderCloseJoinField();
//		String[] fiieldValues = field.split("\\|");
//		String orderno = getParameter(fiieldValues[1]);
		String orderno = getParameter("orderNo");
		if (StringUtils.isBlank(patientId) || StringUtils.isBlank(visitId)) {
			return;
		}
		//获取结果数据
		String result = oclService.getLabOCL(patientId, visitId, orderno);
		renderJson(result);
	}

	/**
	 * @Description
	 * 检查医嘱闭环
	 */
	public void examOCL() {
//		String field = Config.getOrderCloseJoinField();
//		String[] fiieldValues = field.split("\\|");
//		String orderno = getParameter(fiieldValues[1]);
		String orderno = getParameter("orderNo");
		if (StringUtils.isBlank(patientId) || StringUtils.isBlank(visitId)) {
			return;
		}
		//获取结果数据
		String result = oclService.getExamOCL(patientId, visitId, orderno);
		renderJson(result);
	}

	/**
	 * @Description
	 * 获取手术医嘱闭环
	 */
	public void operOCL() {
		String orderno = getParameter("orderNo");
		if (StringUtils.isBlank(patientId) || StringUtils.isBlank(visitId)) {
			return;
		}
		//获取结果数据
		String result = oclService.getOperOCL(patientId, visitId, orderno);
		renderJson(result);
	}

	/**
	 * @Description
	 * 当前视图 - 末次住院手术医嘱闭环
	 */
	public void getCvOCL() {
		String result="";
		if(StringUtils.isNotBlank(patientId))
			result = oclService.getCVOperOCL(patientId, visitId);
		renderJson(StringUtils.isBlank(result) ? "[]" : result);
	}

	/**
	 * @Description
	 * 输血闭环
	 */
	public void bloodOCL() {
		//用血次数编号
		String timesno = getParameter("timesNo");
		if (StringUtils.isBlank(patientId) || StringUtils.isBlank(visitId)) {
			return;
		}
		//获取结果数据
		String result = oclService.getBloodOCL(patientId, visitId, timesno);
		renderJson(result);
	}

}
