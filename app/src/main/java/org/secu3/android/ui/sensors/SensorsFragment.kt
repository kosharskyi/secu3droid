/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */

package org.secu3.android.ui.sensors

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import org.secu3.android.databinding.FragmentSensorsBinding
import java.util.Locale

class SensorsFragment : Fragment() {

    private val mViewModel: SensorsViewModel by viewModels( ownerProducer = { requireParentFragment() } )

    private var mBinding: FragmentSensorsBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentSensorsBinding.inflate(inflater, container, false)
        return mBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.sensorsLiveData.observe(viewLifecycleOwner) {
            mBinding?.apply {
                rpm.title.text = "Обороты, об/мин:"
                rpm.value.text = it.rpm.toString()

                pressure.title.text = "Абсолютное давление, кПа:"
                pressure.value.text = String.format(Locale.US, "%.1f", it.map)

                voltage.title.text = "Напряжение борт. сети, В:"
                voltage.value.text = String.format(Locale.US, "%.1f", it.voltage)

                temperature.title.text = "Температура ОЖ, °C:"
                temperature.value.text =  String.format(Locale.US, "%.1f", it.temperature)

                advAngle.title.text = "Угол опережения, град.:"
                advAngle.value.text = String.format(Locale.US, "%.1f", it.currentAngle)

                knockRetard.title.text = "Корр. УОЗ от ДД, град.:"
                knockRetard.value.text = String.format(Locale.US, "%.1f", it.knockRetard)

                knockValue.title.text = "Сигнал детонации, В:"
                knockValue.value.text = String.format(Locale.US, "%.3f", it.knockValue)

                airFlow.title.text = "Расход воздуха, N кривой:"
                airFlow.value.text = it.airflow.toString()

                tps.title.text = "Дроссельная заслонка, %:"
                tps.value.text = String.format(Locale.US, "%.1f", it.tps)

                add1.title.text = "Вход 1, В:"
                add1.value.text = String.format(Locale.US, "%.3f", it.addI1)

                add2.title.text = "Вход 2, В:"
                add2.value.text = String.format(Locale.US, "%.3f", it.addI2)

                chokePosition.title.text = "ВЗ/РДВ, %:"
                chokePosition.value.text = String.format(Locale.US, "%.1f", it.chokePosition)

                gasDosePosition.title.text = "Дозатор газа, %: "
                gasDosePosition.value.text = it.gasDosePosition.toString()

                synthLoad.title.text = "Синтет. нагрузка:"
                synthLoad.value.text = String.format(Locale.US, "%.1f", it.load)

                speed.title.text = "Скорость авто, км/ч:"
                speed.value.text = String.format(Locale.US, "%.1f", it.speed)

                distance.title.text = "Пробег, км:"
                distance.value.text = String.format(Locale.US, "%.1f", it.distance)

                fuelInj.title.text = "Расход топлива, Гц:"
                fuelInj.value.text = it.fuelFlowFrequency.toString()

                airTemp.title.text = "Температура ДТВ, °C:"
                airTemp.value.text = String.format(Locale.US, "%.1f", it.airtempSensor)

                lambdaCorr.title.text = "Лямбда-коррекция, %:"
                lambdaCorr.value.text = String.format(Locale.US, "%.2f", it.lambda[0])

                injPw.title.text = "Длительность впрыска, мс:"
                injPw.value.text = String.format(Locale.US, "%.2f", it.injPw)

                tpsDot.title.text = "Скорость ДЗ, %/сек:"
                tpsDot.value.text = it.tpsdot.toString()

                map2.title.text = "ДАД 2, кПа:"
                map2.value.text = String.format(Locale.US, "%.1f", it.map2)

                mapDiff.title.text = "Дифф. давление, кПа:"
                //mapDiff.value.text = String.format(Locale.US, "%.1f", it.mapDiff)

                tmp2.title.text = "ДТВ 2, °C:"
                tmp2.value.text = String.format(Locale.US, "%.1f", it.tmp2)

                afr.title.text = "Воздух/топливо ШДК, в/т:"
                afr.value.text = String.format(Locale.US, "%.1f", it.sensAfr[0])

                consFuel.title.text = "Расход топлива, Л/100км:"
                consFuel.value.text = String.format(Locale.US, "%.2f", it.cons_fuel)

                grts.title.text = "ДТГР, °C:"
                grts.value.text = String.format(Locale.US, "%.1f", it.grts)

                ftls.title.text = "Уровень топлива, Л:"
                ftls.value.text = String.format(Locale.US, "%.1f", it.ftls)

                egts.title.text = "Темп. выхл. газов, °C:"
                egts.value.text = String.format(Locale.US, "%.1f", it.egts)

                ops.title.text = "Давление масла, кг/см2:"
                ops.value.text = String.format(Locale.US, "%.1f", it.ops)

                injDuty.title.text = "Загрузка форсунок, %:"
                injDuty.value.text = String.format(Locale.US, "%.1f", it.sens_injDuty)

                maf.title.text = "ДМРВ, г/сек:"
                maf.value.text = String.format(Locale.US, "%.1f", it.sens_maf)

                ventDuty.title.text = "Скважность Вент., %:"
                ventDuty.value.text = it.ventDuty.toString()

                mapDot.title.text = "Скорость ДАД, %/сек:"
                mapDot.value.text = it.mapdot.toString()

                fts.title.text = "Температура топлива, °C:"
                fts.value.text = String.format(Locale.US, "%.1f", it.fts)

                lambdaCorr2.title.text = "Лямбда-коррекция 2, %:"
                //lambdaCorr2.value.text = String.format(Locale.US, "%.1f", it.lambda)

                afrDifference.title.text = "Разность AFR"
                afrDifference.value.text = ""

                afrDifference2.title.text = "Разность AFR2"
                afrDifference2.value.text = ""

                beginInjPhase.title.text = "Фаза начала впрыска"
                beginInjPhase.value.text = ""

                endInjPhase.title.text = "Фаза конца впрыска"
                endInjPhase.value.text = ""

                afrTable.title.text = "Воздух/топливо табл."
                afrTable.value.text = ""

                afr2.title.text = "Воздух/топливо ШДК2, в/т:"
                afr2.value.text = String.format(Locale.US, "%.1f", it.sensAfr[1])

                /**State sensors*/

                statusGasDosThrottleFlFuel.status1.text = "Газовый клапан"
                statusGasDosThrottleFlFuel.status1.setBackgroundColor(if(it.gasBit > 0) Color.GREEN else Color.LTGRAY)

                statusGasDosThrottleFlFuel.status2.text = "Дроссель"
                statusGasDosThrottleFlFuel.status2.setBackgroundColor(if(it.carbBit > 0) Color.GREEN else Color.LTGRAY)

                statusGasDosThrottleFlFuel.status3.text = "Топл. при ПХХ"
                statusGasDosThrottleFlFuel.status3.setBackgroundColor(if(it.ephhValveBit > 0) Color.GREEN else Color.LTGRAY)

                statusPowerValveStarterAe.status1.text = "Клапан ЭМР"
                statusPowerValveStarterAe.status1.setBackgroundColor(if(it.epmValveBit > 0) Color.GREEN else Color.LTGRAY)
                statusPowerValveStarterAe.status2.text = "Блокир. стартера"
                statusPowerValveStarterAe.status2.setBackgroundColor(if(it.starterBlockBit > 0) Color.GREEN else Color.LTGRAY)
                statusPowerValveStarterAe.status3.text = "Обогащ. при уск."
                statusPowerValveStarterAe.status3.setBackgroundColor(if(it.accelerationEnrichment > 0) Color.GREEN else Color.LTGRAY)

                statusCoolingFanCheckEngineRevLimFuelCut.status1.text = "Вентилятор"
                statusCoolingFanCheckEngineRevLimFuelCut.status1.setBackgroundColor(if(it.coolFanBit > 0) Color.GREEN else Color.LTGRAY)
                statusCoolingFanCheckEngineRevLimFuelCut.status2.text = "Check Engine"
                statusCoolingFanCheckEngineRevLimFuelCut.status2.setBackgroundColor(if(it.checkEngineBit > 0) Color.GREEN else Color.LTGRAY)
                statusCoolingFanCheckEngineRevLimFuelCut.status3.text = "Отсеч. топл. по обр."
                statusCoolingFanCheckEngineRevLimFuelCut.status3.setBackgroundColor(if(it.fc_revlim > 0) Color.GREEN else Color.LTGRAY)

                statusFloodClearSysLockIgnInput.status1.text = "Продувка двиг."
                statusFloodClearSysLockIgnInput.status1.setBackgroundColor(if(it.floodclear > 0) Color.GREEN else Color.LTGRAY)
                statusFloodClearSysLockIgnInput.status2.text = "Система заблок."
                statusFloodClearSysLockIgnInput.status2.setBackgroundColor(if(it.sys_locked > 0) Color.GREEN else Color.LTGRAY)
                statusFloodClearSysLockIgnInput.status3.text = "Вход IGN_I"
                statusFloodClearSysLockIgnInput.status3.setBackgroundColor(if(it.ign_i > 0) Color.GREEN else Color.LTGRAY)

                statusCondEpasAfterstrEnr.status1.text = "Вход COND_I"
                statusCondEpasAfterstrEnr.status1.setBackgroundColor(if(it.cond_i > 0) Color.GREEN else Color.LTGRAY)
                statusCondEpasAfterstrEnr.status2.text = "Вход EPAS_I"
                statusCondEpasAfterstrEnr.status2.setBackgroundColor(if(it.epas_i > 0) Color.GREEN else Color.LTGRAY)
                statusCondEpasAfterstrEnr.status3.text = "Обог. после пуска"
                statusCondEpasAfterstrEnr.status3.setBackgroundColor(if(it.aftstr_enr > 0) Color.GREEN else Color.LTGRAY)

                statusClosedLoopReservReserv.status1.text = "РХХ closed loop"
                statusClosedLoopReservReserv.status1.setBackgroundColor(if(it.iac_closed_loop > 0) Color.GREEN else Color.LTGRAY)

                statusUni1Uni2Uni3.status1.text = "Универ. 1"
                statusUni1Uni2Uni3.status1.setBackgroundColor(if(it.uniOut0Bit > 0) Color.GREEN else Color.LTGRAY)
                statusUni1Uni2Uni3.status2.text = "Универ. 2"
                statusUni1Uni2Uni3.status2.setBackgroundColor(if(it.uniOut1Bit > 0) Color.GREEN else Color.LTGRAY)
                statusUni1Uni2Uni3.status3.text = "Универ. 3"
                statusUni1Uni2Uni3.status3.setBackgroundColor(if(it.uniOut2Bit > 0) Color.GREEN else Color.LTGRAY)

                statusUni4Uni5Uni6.status1.text = "Универ. 4"
                statusUni4Uni5Uni6.status1.setBackgroundColor(if(it.uniOut3Bit > 0) Color.GREEN else Color.LTGRAY)
                statusUni4Uni5Uni6.status2.text = "Универ. 5"
                statusUni4Uni5Uni6.status2.setBackgroundColor(if(it.uniOut4Bit > 0) Color.GREEN else Color.LTGRAY)
                statusUni4Uni5Uni6.status3.text = "Универ. 6"
                statusUni4Uni5Uni6.status3.setBackgroundColor(if(it.uniOut5Bit > 0) Color.GREEN else Color.LTGRAY)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }
}
