#pragma version(1)
#pragma rs java_package_name(com.selfsimilartech.fractaleye)

rs_allocation gOut;
int width;
int height;

void RS_KERNEL compact(uchar4 in, uint32_t x, uint32_t y) {

    rsSetElementAt_uchar(gOut, in.r, width*y + x);

    if ((x & 0x01) == 0 && (y & 0x01) == 0) {
        // uint32_t offset = width*height + width*(y >> 1) + x;
        uint32_t offset = width*(height + (y >> 1)) + x;
        rsSetElementAt_uchar(gOut, in.g, offset);
        rsSetElementAt_uchar(gOut, in.b, offset + 1);
    }

}