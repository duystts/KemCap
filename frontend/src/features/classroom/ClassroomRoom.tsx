import { useEffect, useRef, useState, type FormEvent } from 'react'
import {
  CarouselLayout,
  DisconnectButton,
  FocusLayout,
  FocusLayoutContainer,
  GridLayout,
  isTrackReference,
  LayoutContextProvider,
  MediaDeviceMenu,
  ParticipantTile,
  RoomAudioRenderer,
  StartAudio,
  TrackToggle,
  useChat,
  useConnectionState,
  useCreateLayoutContext,
  usePinnedTracks,
  useTracks,
} from '@livekit/components-react'
import { ConnectionState, RoomEvent, Track } from 'livekit-client'
import { Button } from '@/components/ui/button'

function ChatPanel({ onClose }: { onClose: () => void }) {
  const { chatMessages, send, isSending } = useChat()
  const [text, setText] = useState('')
  const listRef = useRef<HTMLUListElement>(null)

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight })
  }, [chatMessages])

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const message = text.trim()
    if (!message) return
    await send(message)
    setText('')
  }

  return (
    <aside
      aria-label="Trò chuyện"
      className="w-full sm:w-80 shrink-0 flex flex-col border-l border-slate-700 bg-slate-900 text-slate-100 absolute inset-0 sm:static z-20"
    >
      <div className="h-11 px-4 flex items-center justify-between border-b border-slate-700 text-sm font-semibold">
        Trò chuyện
        <button type="button" onClick={onClose} className="text-xs font-medium text-slate-300">
          Đóng
        </button>
      </div>
      <ul ref={listRef} className="flex-1 overflow-y-auto p-4 space-y-3 text-sm">
        {chatMessages.length === 0 && <li className="text-slate-400">Chưa có tin nhắn.</li>}
        {chatMessages.map((m) => (
          <li key={m.id}>
            <p className="text-xs text-slate-400">{m.from?.name || m.from?.identity}</p>
            <p className="whitespace-pre-wrap break-words">{m.message}</p>
          </li>
        ))}
      </ul>
      <form onSubmit={submit} className="p-3 border-t border-slate-700 flex gap-2">
        <input
          aria-label="Nội dung tin nhắn"
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Nhập tin nhắn…"
          className="flex-1 min-w-0 h-9 px-3 rounded-lg bg-slate-800 border border-slate-700 text-sm text-white placeholder:text-slate-400"
        />
        <Button type="submit" size="sm" disabled={!text.trim() || isSending}>
          Gửi
        </Button>
      </form>
    </aside>
  )
}

/** Mất kết nối ngắn thì LiveKit tự thử lại, có thể tới gần một phút trước khi báo ngắt hẳn. */
function ReconnectingNotice() {
  const state = useConnectionState()
  if (state !== ConnectionState.Reconnecting && state !== ConnectionState.SignalReconnecting) {
    return null
  }
  return (
    <p
      role="status"
      className="absolute top-3 left-1/2 -translate-x-1/2 z-30 rounded-lg bg-amber-100 px-3 py-1.5 text-sm text-amber-900"
    >
      Đang kết nối lại…
    </p>
  )
}

function ControlBar({ chatOpen, onToggleChat }: { chatOpen: boolean; onToggleChat: () => void }) {
  return (
    <div className="lk-control-bar flex-wrap">
      <div className="lk-button-group">
        <TrackToggle source={Track.Source.Microphone}>Micro</TrackToggle>
        <div className="lk-button-group-menu">
          <MediaDeviceMenu kind="audioinput" aria-label="Chọn micro" />
        </div>
      </div>
      <div className="lk-button-group">
        <TrackToggle source={Track.Source.Camera}>Camera</TrackToggle>
        <div className="lk-button-group-menu">
          <MediaDeviceMenu kind="videoinput" aria-label="Chọn camera" />
        </div>
      </div>
      <TrackToggle source={Track.Source.ScreenShare} captureOptions={{ audio: true }}>
        Chia sẻ màn hình
      </TrackToggle>
      <button type="button" className="lk-button" aria-pressed={chatOpen} onClick={onToggleChat}>
        Trò chuyện
      </button>
      <DisconnectButton>Rời phòng</DisconnectButton>
      <StartAudio label="Bật âm thanh" />
    </div>
  )
}

/**
 * Thay `VideoConference` của LiveKit: prefab đó viết cứng nhãn tiếng Anh và không có tùy chọn dịch
 * (FR-012, T079), nên ghép lại từ các thành phần công khai theo đúng cấu trúc của nó.
 */
export default function ClassroomRoom() {
  const [chatOpen, setChatOpen] = useState(false)
  const layoutContext = useCreateLayoutContext()
  const tracks = useTracks(
    [
      { source: Track.Source.Camera, withPlaceholder: true },
      { source: Track.Source.ScreenShare, withPlaceholder: false },
    ],
    { updateOnlyOn: [RoomEvent.ActiveSpeakersChanged], onlySubscribed: false },
  )
  const focusTrack = usePinnedTracks(layoutContext)?.[0]

  // Có người chia sẻ màn hình thì đưa màn hình lên lớn; ngừng chia sẻ thì trở lại lưới
  const shared = tracks.filter(isTrackReference).find((t) => t.source === Track.Source.ScreenShare)
  // Phía người xem phải chờ subscribe xong mới ghim được; `tracks` chỉ đổi khi có sự kiện của room
  const sharedKey = shared
    ? `${shared.publication.trackSid}_${shared.publication.isSubscribed}`
    : ''
  const subscribed = !!shared?.publication.isSubscribed
  const pinDispatch = layoutContext.pin.dispatch
  useEffect(() => {
    if (shared && subscribed) {
      pinDispatch?.({ msg: 'set_pin', trackReference: shared })
    } else if (!shared) {
      pinDispatch?.({ msg: 'clear_pin' })
    }
    // `shared` là object mới sau mỗi lần render nên theo dõi bằng khóa
  }, [sharedKey, pinDispatch]) // eslint-disable-line react-hooks/exhaustive-deps

  const others = tracks.filter((t) => t !== focusTrack)

  return (
    <LayoutContextProvider value={layoutContext}>
      <div className="h-full w-full flex relative">
        <ReconnectingNotice />
        <div className="flex-1 min-w-0 flex flex-col">
          <div className="flex-1 min-h-0">
            {focusTrack ? (
              <div className="lk-focus-layout-wrapper h-full">
                <FocusLayoutContainer>
                  <CarouselLayout tracks={others}>
                    <ParticipantTile />
                  </CarouselLayout>
                  <FocusLayout trackRef={focusTrack} />
                </FocusLayoutContainer>
              </div>
            ) : (
              <div className="lk-grid-layout-wrapper h-full">
                <GridLayout tracks={tracks}>
                  <ParticipantTile />
                </GridLayout>
              </div>
            )}
          </div>
          <ControlBar chatOpen={chatOpen} onToggleChat={() => setChatOpen((open) => !open)} />
        </div>
        {chatOpen && <ChatPanel onClose={() => setChatOpen(false)} />}
      </div>
      <RoomAudioRenderer />
    </LayoutContextProvider>
  )
}
