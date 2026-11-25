from pathlib import Path
p = Path("qa-tests/src/AlmacenQaSuite.java")
t = p.read_text(encoding="utf-8")
# Only replace fail("ID" helper calls, not Assertions.fail
import re
t2 = re.sub(r'(?<!Assertions\.)\bfail\("([A-Z0-9]+)"', r'failCase("\1"', t)
t2 = t2.replace("failCaseCase", "failCase")
p.write_text(t2, encoding="utf-8")
print("done", t2.count("failCase("))
