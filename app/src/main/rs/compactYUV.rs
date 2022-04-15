#pragma version(1)
#pragma rs java_package_name(com.selfsimilartech.fractaleye)

rs_allocation yAlloc;
rs_allocation uvAlloc;
int width;
int height;

void RS_KERNEL compact(uchar4 in, uint32_t x, uint32_t y) {

    rsSetElementAt_uchar(yAlloc, in.r, width*y + x);

    if ((x & 0x01) == 0 && (y & 0x01) == 0) {

        uint32_t uOffset = width*(y >> 1) + x;
        uint32_t vOffset = uOffset + 1;
        rsSetElementAt_uchar(uvAlloc, in.g, uOffset);
        rsSetElementAt_uchar(uvAlloc, in.b, vOffset);

    }

}