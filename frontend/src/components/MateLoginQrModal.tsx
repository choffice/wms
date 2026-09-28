import { QRCodeSVG } from 'qrcode.react'
import { X } from 'lucide-react'

export function MateLoginQrModal({
                                     open,
                                     onClose
                                 }: {
    open: boolean
    onClose: () => void
}) {
    if (!open) return null

    const baseUrl =
        import.meta.env.VITE_MATE_BASE_URL ||
        window.location.origin

    const url =
        `${baseUrl.replace(/\/$/, '')}/mate/login`

    return (
        <div className="qr-backdrop">
            <div className="qr-modal">
                <button onClick={onClose}>
                    <X size={18}/>
                </button>

                <h3>MATE 모바일 로그인</h3>
                <QRCodeSVG value={url} size={190}/>
                <small>{url}</small>
            </div>
        </div>
    )
}