import re

html_sample = r'''
function do_pl(){ console.log("ab:"+hasAdblock);if(!oldIE&&hasMP4Video){if(hasAdblock){$("#jw").html("<video id=\"flvv\" preload=\"none\" poster=\"//s32.bigcdn.cc/pubs/6ac4cf3cb39386.70865805/main.jpg\" controls style=\"width:100%; height:100%;\" src=\"\"><source src=\"//s32.bigcdn.cc/pubs/6ac4cf3cb39386.70865805/360.mp4\" title=\"360p\" type=\"video/mp4\" /></video>"); }else{ $("#jw").html("<video id=\"flvv\" preload=\"none\" poster=\"//s32.bigcdn.cc/pubs/6ac4cf3cb39386.70865805/main.jpg\" controls style=\"width:100%; height:100%;\" src=\"\"><source src=\"//s32.bigcdn.cc/pubs/6ac4cf3cb39386.70865805/360.mp4\" title=\"360p\" type=\"video/mp4\" /><source src=\"//s32.bigcdn.cc/pubs/6ac4cf3cb39386.70865805/720.mp4\" title=\"720p HD\" type=\"video/mp4\" /><source src=\"//s32.bigcdn.cc/pubs/6ac4cf3cb39386.70865805/1080.mp4\" title=\"1080p Full HD\" type=\"video/mp4\" /></video>");}if(hasAdblock){$("#ovrl_ab").show();$("#ab_wait").hide();$("#ab_close").show();}else{hide_ov();}$("#flvv").on("error",function(){ if($("#flvv").attr("src")!=""){$("#ovrl_err").show();} });}else{$("#jw").css("background","#000").html("<span style='color:#fff'>Your browser has not supported. Video can not be played.<br />You can download it: <a href='//s32.bigcdn.cc/pubs/6ac4cf3cb39386.70865805/360.mp4' style='color:#ddd'>360p</a> : <a href='//s32.bigcdn.cc/pubs/6ac4cf3cb39386.70865805/720.mp4' style='color:#ddd'>720p HD</a> : <a href='//s32.bigcdn.cc/pubs/6ac4cf3cb39386.70865805/1080.mp4' style='color:#ddd'>1080p Full HD</a></span>");} }; do_pl();
'''

# New regex pattern supporting both unescaped and escaped quotes:
pattern = re.compile(r'<source\s+src=[\\"]*([^\\"\'>]+)[\\"]*(?:\s+title=[\\"]*([^\\"\'>]+)[\\"]*)?', re.IGNORECASE)
matches = pattern.findall(html_sample)
print("Matches found:", len(matches))
for m in matches:
    print("  Source:", m[0], "| Title:", m[1])
