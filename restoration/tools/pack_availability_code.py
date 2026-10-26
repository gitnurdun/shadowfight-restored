"""ARM64 local-pack predicate for this exact IL2CPP build.

Use the game's own bundle-directory getter and imported libc access function.
No managed objects are allocated by this replacement. Bundle integrity remains
the responsibility of the original attachment code and verified bootstrap.
"""
from keystone import Ks,KS_ARCH_ARM64,KS_MODE_LITTLE_ENDIAN
RVA=0x325f148
GET_BUNDLE_DIRECTORY=0x3104cb0
ACCESS_GOT=0x41909e0
ASSEMBLY='''
stp x29, x30, [sp, #-32]!
stp x19, x20, [sp, #16]
sub sp, sp, #512
cbz x1, failed
mov x19, x1
mov x0, #0
bl 0x3104cb0
cbz x0, failed
mov x2, sp
ldr w4, [x0, #16]
cbz w4, failed
cmp w4, #300
b.hi failed
add x0, x0, #20
copy_directory:
ldrh w5, [x0], #2
cmp w5, #127
b.hi failed
strb w5, [x2], #1
subs w4, w4, #1
b.ne copy_directory
mov w5, #47
strb w5, [x2], #1
ldr w4, [x19, #16]
cbz w4, failed
cmp w4, #200
b.hi failed
add x19, x19, #20
mov w6, #47
copy_name:
ldrh w5, [x19], #2
cmp w5, #32
b.lo failed
cmp w5, #127
b.hi failed
cmp w5, #46
b.ne check_backslash
cmp w6, #47
b.eq failed
cmp w4, #1
b.eq failed
ldrh w7, [x19]
cmp w7, #46
b.eq failed
cmp w7, #47
b.eq failed
check_backslash:
cmp w5, #92
b.eq failed
mov w6, w5
strb w5, [x2], #1
subs w4, w4, #1
b.ne copy_name
strb wzr, [x2]
mov x0, sp
mov w1, #0
adrp x16, 0x4190000
ldr x16, [x16, #0x9e0]
blr x16
cmp w0, #0
cset w0, eq
b finished
failed:
mov w0, #0
finished:
add sp, sp, #512
ldp x19, x20, [sp, #16]
ldp x29, x30, [sp], #32
ret
'''

def assemble():
    code=bytes(Ks(KS_ARCH_ARM64,KS_MODE_LITTLE_ENDIAN).asm(ASSEMBLY,addr=RVA)[0])
    assert len(code)<=0x325f328-RVA
    return code
