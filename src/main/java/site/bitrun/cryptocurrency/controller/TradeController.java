package site.bitrun.cryptocurrency.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import site.bitrun.cryptocurrency.domain.HoldCrypto;
import site.bitrun.cryptocurrency.domain.Member;
import site.bitrun.cryptocurrency.dto.BuyCryptoForm;
import site.bitrun.cryptocurrency.dto.HoldCryptoDto;
import site.bitrun.cryptocurrency.dto.SellCryptoForm;
import site.bitrun.cryptocurrency.global.api.upbit.domain.UpbitMarket;
import site.bitrun.cryptocurrency.global.api.upbit.service.UpbitService;
import site.bitrun.cryptocurrency.repository.HoldCryptoRepository;
import site.bitrun.cryptocurrency.service.HoldCryptoService;
import site.bitrun.cryptocurrency.service.MemberService;
import site.bitrun.cryptocurrency.session.SessionConst;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class TradeController {

    private final UpbitService upbitService;
    private final HoldCryptoService holdCryptoService;
    private final MemberService memberService;
    private final HoldCryptoRepository holdCryptoRepository;

    @Autowired
    public TradeController(UpbitService upbitService, HoldCryptoService holdCryptoService, MemberService memberService, HoldCryptoRepository holdCryptoRepository) {
        this.upbitService = upbitService;
        this.holdCryptoService = holdCryptoService;
        this.memberService = memberService;
        this.holdCryptoRepository = holdCryptoRepository;
    }

    // 공통 model 정보
    @ModelAttribute
    public void tradeInfo(Model model, HttpServletRequest request) {

        // 오른쪽 side nav 를 위한 전체 리스트
        List<UpbitMarket> upbitMarketList = upbitService.getUpbitMarketList();
        model.addAttribute(upbitMarketList);

        // 보유자산(매수가능자산 KRW)
        HttpSession session = request.getSession(false);
        if (session != null) {
            Member loginMember = (Member) session.getAttribute(SessionConst.LOGIN_MEMBER);
            if (loginMember != null) {
                Member memberInfo = memberService.getMemberInfo(loginMember.getId());
                long memberAsset = memberInfo.getAsset();
                model.addAttribute("memberAsset", memberAsset);
            }
        }
        // upbit websocket 요청 json 부분 - 전체 암호화폐 list json 요청에 넣어줄 것임
        List<String> marketListString = new ArrayList<>();
        for (UpbitMarket upbitMarket : upbitMarketList) {
            marketListString.add(upbitMarket.getMarket());
        }
        model.addAttribute("marketListString", marketListString);
    }

    // 거래소 view
    @GetMapping("/trade/order")
    public String viewOrderPage(@ModelAttribute BuyCryptoForm buyCryptoForm,
                                @ModelAttribute SellCryptoForm sellCryptoForm,
                                Model model, HttpServletRequest request) {

        return "trade/order";
    }

    // 매수
    @PostMapping("/trade/order/buy")
    public String requestCryptoBuy(@Validated @ModelAttribute BuyCryptoForm buyCryptoForm, BindingResult bindingResult,
                                   @ModelAttribute SellCryptoForm sellCryptoForm,
                                   HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            return "trade/order";
        }

        // 매수 금액 유효성 체크 - 숫자만 입력
        Long buyKrw = null;
        try {
            buyKrw = Long.parseLong(buyCryptoForm.getBuyKrw().replaceAll(",", "")); // 금액에 ',' 제거
        } catch (Exception ex) {
            log.info("매수 금액 숫자 Exception");
            bindingResult.rejectValue("buyKrw", "onlyNumber", "숫자만 입력해주세요.");
            return "trade/order";
        }

        // 매수 금액이 음수인 경우 체크
        if (buyKrw < 0) {
            log.info("매수 금액 음수 Exception");
            bindingResult.rejectValue("buyKrw", "negativeNumber", "0보다 큰 수를 입력해주세요.");
            return "trade/order";
        }

        // 로그인 회원 정보
        HttpSession session = request.getSession(false);
        Member loginMember = (Member) session.getAttribute(SessionConst.LOGIN_MEMBER);

        // 보유자산 보다 매수 금액 이 큰 경우 체크
        Member memberInfo = memberService.getMemberInfo(loginMember.getId());
        long memberAsset = memberInfo.getAsset(); // 보유 KRW (매수 가능 자산)
        if (buyKrw > memberAsset) {
            log.info("매수 금액 초과 Exception");
            bindingResult.rejectValue("buyKrw", "overBuyKrw", "매수 가능 금액보다 클 수 없습니다.");
            return "trade/order";
        }

        holdCryptoService.buyCrypto(loginMember.getId(), buyCryptoForm.getBuyMarketCode(), buyKrw);

        return "redirect:/trade/hold/crypto";
    }

    // 매도
    @PostMapping("/trade/order/sell")
    public String requestCryptoSell(@Validated @ModelAttribute SellCryptoForm sellCryptoForm, BindingResult bindingResult,
                                    @ModelAttribute BuyCryptoForm buyCryptoForm,
                                    HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            return "trade/order";
        }

        // 로그인 회원 정보
        HttpSession session = request.getSession(false);
        Member loginMember = (Member) session.getAttribute(SessionConst.LOGIN_MEMBER);

        // 매도 개수 유효성 체크 - 숫자만 입력
        Double sellCount = null;
        try {
            sellCount = Double.parseDouble(sellCryptoForm.getSellCount());
        } catch (Exception ex) {
            log.info("매도 금액 숫자 Exception");
            bindingResult.rejectValue("sellCount", "onlyNumber", "숫자만 입력해주세요.");
            return "trade/order";
        }

        // 매도 개수가 음수인 경우 체크
        if (sellCount < 0) {
            log.info("매도 개수 음수 Exception");
            bindingResult.rejectValue("sellCount", "negativeNumber", "0보다 큰 수를 입력해주세요.");
            return "trade/order";
        }

        // 보유하고 있지 않은 암호화폐 매도 체크
        UpbitMarket findUpbitMarketOne = upbitService.getUpbitMarketOne(sellCryptoForm.getSellMarketCode());
        HoldCrypto findHoldCrypto = holdCryptoRepository.findByMemberIdAndUpbitMarketId(loginMember.getId(), findUpbitMarketOne.getId());
        if (findHoldCrypto == null) {
            log.info("보유하고 있지 않은 암호화폐 Exception");
            bindingResult.rejectValue("sellCount", "negativeHold", "암호화폐를 보유중이지 않습니다.");
            return "trade/order";
        }

        // 매도 개수가 보유 개수보다 많은 경우 체크
        if (findHoldCrypto.getBuyCryptoCount() < sellCount) {
            log.info("매도 개수 초과 Exception");
            bindingResult.rejectValue("sellCount", "negativeNumber", "매도 가능 개수보다 클 수 없습니다.");
            return "trade/order";
        }

        holdCryptoService.sellCrypto(loginMember.getId(), sellCryptoForm.getSellMarketCode(), sellCount);

        return "redirect:/trade/hold/crypto";
    }

    // 보유자산 view
    @GetMapping("/trade/hold/crypto")
    public String viewHoldCryptoPage(Model model, HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        Member loginMember = (Member) session.getAttribute(SessionConst.LOGIN_MEMBER);

        // 보유 암호화폐 list
        List<HoldCryptoDto> holdCryptoList = holdCryptoService.getHoldCryptoList(loginMember.getId());
        model.addAttribute("holdCryptoList", holdCryptoList);

        // 총매수금액 (KRW)
        long totalBuyKrw = 0;
        for (HoldCryptoDto holdCryptoDto : holdCryptoList) {
            totalBuyKrw += holdCryptoDto.getBuyTotalKrw();
        }
        model.addAttribute("totalBuyKrw", totalBuyKrw);


        // 보유중인 암호화폐 upbit websocket 요청 json 부분 - 암호화폐 list json 요청
        List<String> marketArrayList = new ArrayList<>();
        for (HoldCryptoDto holdCrypto : holdCryptoList) {
            marketArrayList.add(holdCrypto.getMarketCode());
        }
        model.addAttribute("marketArrayList", marketArrayList);

        return "trade/holdCrypto";
    }

}
